package com.educompus.service;

import com.educompus.model.*;
import com.educompus.repository.SessionActionRepository;
import com.educompus.repository.SessionLiveRepository;
import com.educompus.repository.SessionParticipantRepository;

import java.util.List;

/**
 * Service métier avancé pour les sessions live.
 *
 * <p>Expose une API événementielle (pas CRUD classique) :
 * <ul>
 *   <li>{@link #startSession(int)}       — démarre une session</li>
 *   <li>{@link #endSession(int)}         — termine une session</li>
 *   <li>{@link #joinSession(int, int, String)}  — étudiant rejoint</li>
 *   <li>{@link #leaveSession(int, int)}  — étudiant quitte</li>
 *   <li>{@link #raiseHand(int, int, String)}    — lève la main</li>
 *   <li>{@link #lowerHand(int, int, String)}    — baisse la main</li>
 *   <li>{@link #requestSpeak(int, int, String)} — demande la parole</li>
 *   <li>{@link #grantSpeak(int, int)}    — accorde la parole</li>
 *   <li>{@link #revokeSpeak(int, int)}   — retire la parole</li>
 *   <li>{@link #getLiveStatus(int)}      — état temps réel</li>
 * </ul>
 */
public final class SessionLiveMetierService {

    private final SessionLiveRepository       sessionRepo;
    private final SessionParticipantRepository participantRepo;
    private final SessionActionRepository     actionRepo;

    public SessionLiveMetierService() {
        this.sessionRepo     = new SessionLiveRepository();
        this.participantRepo = new SessionParticipantRepository();
        this.actionRepo      = new SessionActionRepository();
    }

    // ── API événementielle ────────────────────────────────────────────────────

    /**
     * Démarre une session (PLANIFIEE → EN_COURS).
     * Enregistre l'événement SESSION_START dans le journal.
     *
     * @param sessionId ID de la session à démarrer
     * @throws IllegalStateException si la session n'est pas dans l'état PLANIFIEE
     */
    public void startSession(int sessionId) {
        SessionLive session = getSessionOuErreur(sessionId);
        if (!session.getStatut().peutTransitionnerVers(SessionStatut.EN_COURS)) {
            throw new IllegalStateException(
                "Impossible de démarrer : statut actuel = " + session.getStatut().libelle());
        }
        session.setStatut(SessionStatut.EN_COURS);
        sessionRepo.modifierSession(session);
        log(sessionId, 0, "Système", ActionType.SESSION_START, "Session démarrée");
    }

    /**
     * Termine une session (EN_COURS → TERMINEE).
     * Marque tous les participants encore présents comme partis.
     * Enregistre l'événement SESSION_END dans le journal.
     *
     * @param sessionId ID de la session à terminer
     * @throws IllegalStateException si la session n'est pas EN_COURS
     */
    public void endSession(int sessionId) {
        SessionLive session = getSessionOuErreur(sessionId);
        if (!session.getStatut().peutTransitionnerVers(SessionStatut.TERMINEE)) {
            throw new IllegalStateException(
                "Impossible de terminer : statut actuel = " + session.getStatut().libelle());
        }
        // Fermer tous les participants encore présents
        participantRepo.leaveAll(sessionId);
        session.setStatut(SessionStatut.TERMINEE);
        sessionRepo.modifierSession(session);
        log(sessionId, 0, "Système", ActionType.SESSION_END,
            "Session terminée — " + participantRepo.getTous(sessionId).size() + " participant(s)");
    }

    /**
     * Un étudiant rejoint la session.
     * La session doit être EN_COURS.
     *
     * @param sessionId   ID de la session
     * @param etudiantId  ID de l'étudiant
     * @param nomEtudiant Nom affiché de l'étudiant
     * @throws IllegalStateException si la session n'est pas active
     */
    public void joinSession(int sessionId, int etudiantId, String nomEtudiant) {
        SessionLive session = getSessionOuErreur(sessionId);
        if (!session.getStatut().peutRejoindre()) {
            throw new IllegalStateException(
                "La session n'est pas active (statut : " + session.getStatut().libelle() + ")");
        }
        SessionParticipant p = new SessionParticipant(sessionId, etudiantId, nomEtudiant);
        participantRepo.join(p);
        log(sessionId, etudiantId, nomEtudiant, ActionType.JOIN, null);
    }

    /**
     * Un étudiant quitte la session.
     * Réinitialise son état interactif (main levée, demande parole).
     *
     * @param sessionId  ID de la session
     * @param etudiantId ID de l'étudiant
     */
    public void leaveSession(int sessionId, int etudiantId) {
        String nom = getNomEtudiant(sessionId, etudiantId);
        participantRepo.leave(sessionId, etudiantId);
        log(sessionId, etudiantId, nom, ActionType.LEAVE, null);
    }

    /**
     * Un étudiant lève la main.
     * La session doit être EN_COURS et l'étudiant doit être présent.
     *
     * @param sessionId   ID de la session
     * @param etudiantId  ID de l'étudiant
     * @param nomEtudiant Nom de l'étudiant
     * @throws IllegalStateException si l'étudiant n'est pas présent
     */
    public void raiseHand(int sessionId, int etudiantId, String nomEtudiant) {
        verifierPresence(sessionId, etudiantId, nomEtudiant);
        List<SessionParticipant> presents = participantRepo.getPresents(sessionId);
        SessionParticipant p = trouverParticipant(presents, etudiantId);
        if (p != null) {
            participantRepo.updateEtatInteractif(sessionId, etudiantId,
                true, p.isDemandeParole(), p.isParoleAccordee());
        }
        log(sessionId, etudiantId, nomEtudiant, ActionType.RAISE_HAND, null);
    }

    /**
     * Un étudiant baisse la main.
     *
     * @param sessionId   ID de la session
     * @param etudiantId  ID de l'étudiant
     * @param nomEtudiant Nom de l'étudiant
     */
    public void lowerHand(int sessionId, int etudiantId, String nomEtudiant) {
        List<SessionParticipant> presents = participantRepo.getPresents(sessionId);
        SessionParticipant p = trouverParticipant(presents, etudiantId);
        if (p != null) {
            participantRepo.updateEtatInteractif(sessionId, etudiantId,
                false, p.isDemandeParole(), p.isParoleAccordee());
        }
        log(sessionId, etudiantId, nomEtudiant, ActionType.LOWER_HAND, null);
    }

    /**
     * Un étudiant demande la parole.
     *
     * @param sessionId   ID de la session
     * @param etudiantId  ID de l'étudiant
     * @param nomEtudiant Nom de l'étudiant
     * @throws IllegalStateException si l'étudiant n'est pas présent
     */
    public void requestSpeak(int sessionId, int etudiantId, String nomEtudiant) {
        verifierPresence(sessionId, etudiantId, nomEtudiant);
        List<SessionParticipant> presents = participantRepo.getPresents(sessionId);
        SessionParticipant p = trouverParticipant(presents, etudiantId);
        if (p != null) {
            participantRepo.updateEtatInteractif(sessionId, etudiantId,
                p.isMainLevee(), true, p.isParoleAccordee());
        }
        log(sessionId, etudiantId, nomEtudiant, ActionType.REQUEST_SPEAK, null);
    }

    /**
     * L'enseignant accorde la parole à un étudiant.
     *
     * @param sessionId  ID de la session
     * @param etudiantId ID de l'étudiant qui reçoit la parole
     */
    public void grantSpeak(int sessionId, int etudiantId) {
        String nom = getNomEtudiant(sessionId, etudiantId);
        List<SessionParticipant> presents = participantRepo.getPresents(sessionId);
        SessionParticipant p = trouverParticipant(presents, etudiantId);
        if (p != null) {
            participantRepo.updateEtatInteractif(sessionId, etudiantId,
                p.isMainLevee(), false, true);
        }
        log(sessionId, etudiantId, nom, ActionType.GRANT_SPEAK, "Parole accordée par l'enseignant");
    }

    /**
     * L'enseignant retire la parole à un étudiant.
     *
     * @param sessionId  ID de la session
     * @param etudiantId ID de l'étudiant
     */
    public void revokeSpeak(int sessionId, int etudiantId) {
        String nom = getNomEtudiant(sessionId, etudiantId);
        List<SessionParticipant> presents = participantRepo.getPresents(sessionId);
        SessionParticipant p = trouverParticipant(presents, etudiantId);
        if (p != null) {
            participantRepo.updateEtatInteractif(sessionId, etudiantId,
                p.isMainLevee(), false, false);
        }
        log(sessionId, etudiantId, nom, ActionType.REVOKE_SPEAK, "Parole retirée par l'enseignant");
    }

    // ── Statut temps réel ─────────────────────────────────────────────────────

    /**
     * Retourne l'état complet en temps réel d'une session.
     *
     * @param sessionId ID de la session
     * @return Un objet {@link LiveStatus} avec toutes les informations dynamiques
     */
    public LiveStatus getLiveStatus(int sessionId) {
        SessionLive session = getSessionOuErreur(sessionId);
        List<SessionParticipant> presents  = participantRepo.getPresents(sessionId);
        List<SessionParticipant> mainsLevees = participantRepo.getMainsLevees(sessionId);
        List<SessionParticipant> demandesParole = participantRepo.getDemandesParole(sessionId);
        List<SessionAction> actionsRecentes = actionRepo.getActionsRecentes(sessionId, 20);
        int totalJoins = actionRepo.count(sessionId, ActionType.JOIN);

        return new LiveStatus(session, presents, mainsLevees, demandesParole,
                              actionsRecentes, totalJoins);
    }

    // ── Lecture simple ────────────────────────────────────────────────────────

    public List<SessionAction> getJournal(int sessionId) {
        return actionRepo.getJournal(sessionId);
    }

    public List<SessionParticipant> getParticipants(int sessionId) {
        return participantRepo.getTous(sessionId);
    }

    public List<SessionLive> getAllSessions() {
        return sessionRepo.getAllSessions();
    }

    public SessionLive getSession(int sessionId) {
        return sessionRepo.getSessionById(sessionId);
    }

    // ── Classe interne : état temps réel ─────────────────────────────────────

    /**
     * Snapshot de l'état en temps réel d'une session live.
     */
    public static final class LiveStatus {
        private final SessionLive              session;
        private final List<SessionParticipant> presents;
        private final List<SessionParticipant> mainsLevees;
        private final List<SessionParticipant> demandesParole;
        private final List<SessionAction>      actionsRecentes;
        private final int                      totalParticipants;

        LiveStatus(SessionLive session,
                   List<SessionParticipant> presents,
                   List<SessionParticipant> mainsLevees,
                   List<SessionParticipant> demandesParole,
                   List<SessionAction> actionsRecentes,
                   int totalParticipants) {
            this.session          = session;
            this.presents         = presents;
            this.mainsLevees      = mainsLevees;
            this.demandesParole   = demandesParole;
            this.actionsRecentes  = actionsRecentes;
            this.totalParticipants = totalParticipants;
        }

        public SessionLive              getSession()          { return session; }
        public List<SessionParticipant> getPresents()         { return presents; }
        public List<SessionParticipant> getMainsLevees()      { return mainsLevees; }
        public List<SessionParticipant> getDemandesParole()   { return demandesParole; }
        public List<SessionAction>      getActionsRecentes()  { return actionsRecentes; }
        public int                      getNbPresents()       { return presents.size(); }
        public int                      getNbMainsLevees()    { return mainsLevees.size(); }
        public int                      getNbDemandesParole() { return demandesParole.size(); }
        public int                      getTotalParticipants(){ return totalParticipants; }
        public boolean                  estActive()           { return session != null && session.estActive(); }

        @Override
        public String toString() {
            if (session == null) return "Session inconnue";
            return String.format("[%s] %s — %d présents, %d mains levées, %d demandes parole",
                session.getStatut().libelle(), session.getNomCours(),
                getNbPresents(), getNbMainsLevees(), getNbDemandesParole());
        }
    }

    // ── Utilitaires privés ────────────────────────────────────────────────────

    private SessionLive getSessionOuErreur(int id) {
        SessionLive s = sessionRepo.getSessionById(id);
        if (s == null) throw new IllegalStateException("Session introuvable : id=" + id);
        return s;
    }

    private void verifierPresence(int sessionId, int etudiantId, String nom) {
        if (!participantRepo.estPresent(sessionId, etudiantId)) {
            throw new IllegalStateException(
                nom + " n'est pas présent dans la session " + sessionId);
        }
    }

    private String getNomEtudiant(int sessionId, int etudiantId) {
        return participantRepo.getPresents(sessionId).stream()
            .filter(p -> p.getEtudiantId() == etudiantId)
            .map(SessionParticipant::getNomEtudiant)
            .findFirst()
            .orElse("Étudiant #" + etudiantId);
    }

    private SessionParticipant trouverParticipant(List<SessionParticipant> list, int etudiantId) {
        return list.stream()
            .filter(p -> p.getEtudiantId() == etudiantId)
            .findFirst()
            .orElse(null);
    }

    private void log(int sessionId, int etudiantId, String nom, ActionType type, String details) {
        try {
            actionRepo.enregistrer(new SessionAction(sessionId, etudiantId, nom, type, details));
        } catch (Exception e) {
            // Le logging ne doit jamais bloquer l'action principale
            System.err.println("[SessionLive] Erreur log: " + e.getMessage());
        }
    }
}

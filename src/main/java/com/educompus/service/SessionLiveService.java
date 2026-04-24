package com.educompus.service;

import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;
import com.educompus.repository.SessionLiveRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Service de logique métier pour les sessions live.
 * Orchestre les opérations CRUD avec validation et gère les transitions de statut.
 */
public final class SessionLiveService {

    private final SessionLiveRepository repository;
    private final SessionLiveValidationService validationService;

    public SessionLiveService() {
        this.repository = new SessionLiveRepository();
        this.validationService = null; // méthodes statiques
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * Ajoute une nouvelle session live après validation.
     * @param session La session à ajouter
     * @throws IllegalArgumentException si la validation échoue
     */
    public void ajouterSession(SessionLive session) {
        ValidationResult validation = SessionLiveValidationService.validerSession(session);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.allErrors());
        }
        // Statut par défaut à la création
        if (session.getStatut() == null) {
            session.setStatut(SessionStatut.PLANIFIEE);
        }
        repository.ajouterSession(session);
    }

    /**
     * Modifie une session live existante après validation.
     * @param session La session avec les nouvelles données
     * @throws IllegalArgumentException si la validation échoue
     * @throws IllegalStateException si la session n'existe pas
     */
    public void modifierSession(SessionLive session) {
        if (session.getId() <= 0) {
            throw new IllegalArgumentException("L'ID de la session est invalide.");
        }
        ValidationResult validation = SessionLiveValidationService.validerSession(session);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.allErrors());
        }
        repository.modifierSession(session);
    }

    /**
     * Supprime une session live.
     * @param id L'ID de la session à supprimer
     * @throws IllegalArgumentException si l'ID est invalide
     */
    public void supprimerSession(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID de la session est invalide.");
        }
        repository.supprimerSession(id);
    }

    /**
     * Récupère toutes les sessions live.
     */
    public List<SessionLive> getAllSessions() {
        return repository.getAllSessions();
    }

    /**
     * Récupère une session live par son ID.
     * @param id L'ID de la session
     * @return La session, ou null si non trouvée
     */
    public SessionLive getSessionById(int id) {
        if (id <= 0) return null;
        return repository.getSessionById(id);
    }

    // ── Logique métier ────────────────────────────────────────────────────────

    /**
     * Démarre une session (PLANIFIEE → EN_COURS).
     * @param id L'ID de la session à démarrer
     * @throws IllegalStateException si la transition est invalide
     */
    public void demarrerSession(int id) {
        SessionLive session = getSessionOuErreur(id);
        if (!session.getStatut().peutTransitionnerVers(SessionStatut.EN_COURS)) {
            throw new IllegalStateException(
                "Impossible de démarrer la session : statut actuel = " + session.getStatut().libelle()
            );
        }
        session.setStatut(SessionStatut.EN_COURS);
        repository.modifierSession(session);
    }

    /**
     * Termine une session (EN_COURS → TERMINEE).
     * @param id L'ID de la session à terminer
     * @throws IllegalStateException si la transition est invalide
     */
    public void terminerSession(int id) {
        SessionLive session = getSessionOuErreur(id);
        if (!session.getStatut().peutTransitionnerVers(SessionStatut.TERMINEE)) {
            throw new IllegalStateException(
                "Impossible de terminer la session : statut actuel = " + session.getStatut().libelle()
            );
        }
        session.setStatut(SessionStatut.TERMINEE);
        repository.modifierSession(session);
    }

    /**
     * Annule une session (PLANIFIEE ou EN_COURS → ANNULEE).
     * @param id L'ID de la session à annuler
     * @throws IllegalStateException si la transition est invalide
     */
    public void annulerSession(int id) {
        SessionLive session = getSessionOuErreur(id);
        if (!session.getStatut().peutTransitionnerVers(SessionStatut.ANNULEE)) {
            throw new IllegalStateException(
                "Impossible d'annuler la session : statut actuel = " + session.getStatut().libelle()
            );
        }
        session.setStatut(SessionStatut.ANNULEE);
        repository.modifierSession(session);
    }

    /**
     * Récupère les sessions filtrées par statut.
     */
    public List<SessionLive> getSessionsByStatut(SessionStatut statut) {
        if (statut == null) return getAllSessions();
        return repository.getSessionsByStatut(statut);
    }

    /**
     * Récupère les sessions d'un cours spécifique.
     */
    public List<SessionLive> getSessionsByCoursId(int coursId) {
        if (coursId <= 0) return List.of();
        return repository.getSessionsByCoursId(coursId);
    }

    /**
     * Récupère les sessions d'aujourd'hui.
     */
    public List<SessionLive> getSessionsAujourdhui() {
        return repository.getSessionsByDate(LocalDate.now());
    }

    /**
     * Recherche des sessions par nom de cours.
     */
    public List<SessionLive> rechercherSessions(String query) {
        return repository.searchSessions(query);
    }

    /**
     * Vérifie si une session peut être rejointe par un étudiant.
     * @param id L'ID de la session
     * @return true si la session est en cours et le lien est valide
     */
    public boolean peutRejoindre(int id) {
        SessionLive session = repository.getSessionById(id);
        return session != null && session.peutEtreRejointe() && session.aLienValide();
    }

    /**
     * Retourne le lien d'une session si elle peut être rejointe.
     * @param id L'ID de la session
     * @return Le lien de la session, ou null si non disponible
     */
    public String getLienSession(int id) {
        SessionLive session = repository.getSessionById(id);
        if (session == null || !session.peutEtreRejointe()) return null;
        return session.getLien();
    }

    /**
     * Compte les sessions actives (EN_COURS).
     */
    public int countSessionsActives() {
        return repository.countSessionsByStatut(SessionStatut.EN_COURS);
    }

    /**
     * Compte les sessions planifiées.
     */
    public int countSessionsPlanifiees() {
        return repository.countSessionsByStatut(SessionStatut.PLANIFIEE);
    }

    // ── Utilitaires privés ────────────────────────────────────────────────────

    private SessionLive getSessionOuErreur(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID de la session est invalide.");
        }
        SessionLive session = repository.getSessionById(id);
        if (session == null) {
            throw new IllegalStateException("Session introuvable avec l'ID : " + id);
        }
        return session;
    }
}

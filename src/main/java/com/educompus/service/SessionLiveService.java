package com.educompus.service;

import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;
import com.educompus.repository.SessionLiveRepository;
import com.educompus.repository.NotificationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service de logique métier pour les sessions live.
 * Orchestre les opérations CRUD avec validation et gère les transitions de statut.
 * Intègre le système de notifications automatiques.
 */
public final class SessionLiveService {

    private final SessionLiveRepository repository;
    private final SessionLiveValidationService validationService;
    private final NotificationRepository notificationRepository;

    public SessionLiveService() {
        this.repository = new SessionLiveRepository();
        this.validationService = null; // méthodes statiques
        this.notificationRepository = new NotificationRepository();
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * Ajoute une nouvelle session live après validation.
     * Planifie automatiquement les notifications.
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
        
        // Ajouter la session
        int sessionId = repository.ajouterSession(session);
        session.setId(sessionId);
        
        // Planifier les notifications automatiquement
        try {
            // Convertir les champs date/heure en LocalDateTime pour les notifications
            if (session.getDate() != null && session.getHeure() != null) {
                LocalDateTime dateDebut = LocalDateTime.of(session.getDate(), session.getHeure());
                session.setDateDebut(dateDebut);
                session.setDateFin(dateDebut.plusHours(1)); // Durée par défaut
                
                notificationRepository.createNotificationStatesForSession(session);
                System.out.println("Notifications planifiées pour la session " + sessionId);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la planification des notifications: " + e.getMessage());
            // Ne pas faire échouer la création de session pour un problème de notification
        }
    }

    /**
     * Modifie une session live existante après validation.
     * Met à jour les notifications si les dates ont changé.
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
        
        // Récupérer l'ancienne session pour comparer les dates
        SessionLive ancienneSession = repository.getSessionById(session.getId());
        boolean datesChangees = false;
        
        if (ancienneSession != null) {
            datesChangees = !ancienneSession.getDate().equals(session.getDate()) || 
                           !ancienneSession.getHeure().equals(session.getHeure());
        }
        
        // Modifier la session
        repository.modifierSession(session);
        
        // Mettre à jour les notifications si les dates ont changé
        if (datesChangees) {
            try {
                // Convertir les champs pour les notifications
                if (session.getDate() != null && session.getHeure() != null) {
                    LocalDateTime dateDebut = LocalDateTime.of(session.getDate(), session.getHeure());
                    session.setDateDebut(dateDebut);
                    session.setDateFin(dateDebut.plusHours(1));
                    
                    notificationRepository.updateNotificationTimesForSession(session);
                    System.out.println("Notifications mises à jour pour la session " + session.getId());
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la mise à jour des notifications: " + e.getMessage());
            }
        }
    }

    /**
     * Supprime une session live.
     * Supprime également les notifications associées.
     * @param id L'ID de la session à supprimer
     * @throws IllegalArgumentException si l'ID est invalide
     */
    public void supprimerSession(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID de la session est invalide.");
        }
        
        // Supprimer les notifications associées
        try {
            notificationRepository.deleteNotificationStatesForSession(id);
            System.out.println("Notifications supprimées pour la session " + id);
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression des notifications: " + e.getMessage());
        }
        
        // Supprimer la session
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

    // ── Méthodes pour le système de notifications ─────────────────────────────

    /**
     * Récupère les sessions à venir dans une plage de temps.
     * Utilisé par le système de notifications.
     * @param from Début de la plage
     * @param to Fin de la plage
     * @return Liste des sessions dans la plage
     */
    public List<SessionLive> getUpcomingSessions(LocalDateTime from, LocalDateTime to) {
        return repository.findUpcomingSessions(from, to);
    }

    /**
     * Planifie manuellement les notifications pour une session existante.
     * Utile pour les sessions créées avant l'activation du système de notifications.
     * @param sessionId ID de la session
     */
    public void planifierNotifications(int sessionId) {
        SessionLive session = repository.findById(sessionId);
        if (session != null) {
            try {
                notificationRepository.createNotificationStatesForSession(session);
                System.out.println("Notifications planifiées manuellement pour la session " + sessionId);
            } catch (Exception e) {
                System.err.println("Erreur lors de la planification manuelle: " + e.getMessage());
            }
        }
    }

    /**
     * Planifie les notifications pour toutes les sessions futures sans notifications.
     * Utile pour la migration vers le système de notifications.
     */
    public void planifierNotificationsPourToutesLesSessions() {
        List<SessionLive> sessionsPlanifiees = getSessionsByStatut(SessionStatut.PLANIFIEE);
        int count = 0;
        
        for (SessionLive session : sessionsPlanifiees) {
            // Vérifier si la session est dans le futur
            if (session.getDate() != null && session.getHeure() != null) {
                LocalDateTime dateDebut = LocalDateTime.of(session.getDate(), session.getHeure());
                if (dateDebut.isAfter(LocalDateTime.now())) {
                    try {
                        session.setDateDebut(dateDebut);
                        session.setDateFin(dateDebut.plusHours(1));
                        notificationRepository.createNotificationStatesForSession(session);
                        count++;
                    } catch (Exception e) {
                        System.err.println("Erreur pour session " + session.getId() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        System.out.println("Notifications planifiées pour " + count + " sessions");
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

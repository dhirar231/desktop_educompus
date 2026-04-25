package com.educompus.debug;

import com.educompus.model.NotificationType;
import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;
import com.educompus.service.NotificationService;
import javafx.application.Platform;

import java.time.LocalDateTime;

/**
 * Classe utilitaire pour tester les notifications depuis l'application principale.
 * Peut être appelée depuis n'importe quel contrôleur.
 */
public class TestNotificationInApp {
    
    /**
     * Teste la notification urgente directement.
     * À appeler depuis un bouton ou une méthode de votre application.
     */
    public static void testUrgentNotificationNow() {
        System.out.println("🚨 TEST NOTIFICATION URGENTE DÉCLENCHÉ");
        
        Platform.runLater(() -> {
            try {
                // Créer une session de test
                SessionLive session = createTestSession();
                
                // Utiliser le service de notification existant
                NotificationService service = new NotificationService();
                service.sendUrgentNotification(session, NotificationType.FIVE_MINUTES);
                
                System.out.println("✅ Notification urgente envoyée !");
                System.out.println("📋 Session: " + session.getTitre());
                System.out.println("👨‍🏫 Enseignant: " + session.getEnseignantNom());
                System.out.println("🕐 Heure: " + session.getDateHeureFormatee());
                
            } catch (Exception e) {
                System.err.println("❌ Erreur lors du test: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Teste la notification info (30 minutes).
     */
    public static void testInfoNotificationNow() {
        System.out.println("📅 TEST NOTIFICATION INFO DÉCLENCHÉ");
        
        Platform.runLater(() -> {
            try {
                SessionLive session = createTestSession();
                
                NotificationService service = new NotificationService();
                service.sendInfoNotification(session, NotificationType.THIRTY_MINUTES);
                
                System.out.println("✅ Notification info envoyée !");
                
            } catch (Exception e) {
                System.err.println("❌ Erreur lors du test: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Teste les deux types de notifications avec un délai.
     */
    public static void testBothNotifications() {
        System.out.println("🚀 TEST DES DEUX NOTIFICATIONS");
        
        // Notification info d'abord
        testInfoNotificationNow();
        
        // Notification urgente après 5 secondes
        Platform.runLater(() -> {
            try {
                Thread.sleep(5000);
                testUrgentNotificationNow();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private static SessionLive createTestSession() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionTime = now.plusMinutes(5);
        
        SessionLive session = new SessionLive();
        session.setId(999);
        session.setNomCours("Session Test Notification");
        session.setTitre("Session Test Notification");
        session.setCoursTitre("Cours de Test");
        session.setEnseignantNom("Prof. Test");
        session.setLien("https://meet.google.com/test-notification");
        session.setLienSession("https://meet.google.com/test-notification");
        session.setDate(sessionTime.toLocalDate());
        session.setHeure(sessionTime.toLocalTime());
        session.setDateDebut(sessionTime);
        session.setDateFin(sessionTime.plusHours(1));
        session.setStatut(SessionStatut.PLANIFIEE);
        
        return session;
    }
}
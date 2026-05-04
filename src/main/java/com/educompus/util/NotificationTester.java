package com.educompus.util;

import com.educompus.model.NotificationType;
import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;
import com.educompus.service.NotificationService;
import javafx.application.Platform;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Utilitaire pour tester les notifications manuellement.
 */
public final class NotificationTester {
    
    private NotificationTester() {}
    
    /**
     * Teste une notification d'information (30 minutes).
     */
    public static void testInfoNotification() {
        SessionLive testSession = createTestSession("Test Session - Info", 30);
        
        NotificationService service = new NotificationService();
        service.sendInfoNotification(testSession, NotificationType.THIRTY_MINUTES);
        
        System.out.println("✅ Notification info envoyée pour test");
    }
    
    /**
     * Teste une notification urgente (5 minutes).
     */
    public static void testUrgentNotification() {
        SessionLive testSession = createTestSession("Test Session - Urgent", 5);
        
        NotificationService service = new NotificationService();
        service.sendUrgentNotification(testSession, NotificationType.FIVE_MINUTES);
        
        System.out.println("✅ Notification urgente envoyée pour test");
    }
    
    /**
     * Teste les deux types de notifications.
     */
    public static void testBothNotifications() {
        System.out.println("🧪 Test des notifications...");
        
        // Attendre que JavaFX soit initialisé
        Platform.runLater(() -> {
            testInfoNotification();
            
            // Attendre 2 secondes puis tester la notification urgente
            Platform.runLater(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                testUrgentNotification();
            });
        });
    }
    
    /**
     * Crée une session de test.
     */
    private static SessionLive createTestSession(String nom, int minutesAvant) {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime heureSession = maintenant.plusMinutes(minutesAvant);
        
        SessionLive session = new SessionLive();
        session.setId(999); // ID de test
        session.setNomCours(nom);
        session.setTitre(nom);
        session.setCoursTitre("Cours de Test");
        session.setEnseignantNom("Prof. Test");
        session.setLien("https://meet.google.com/test-session");
        session.setLienSession("https://meet.google.com/test-session");
        session.setDate(heureSession.toLocalDate());
        session.setHeure(heureSession.toLocalTime());
        session.setDateDebut(heureSession);
        session.setDateFin(heureSession.plusHours(1));
        session.setStatut(SessionStatut.PLANIFIEE);
        
        return session;
    }
    
    /**
     * Méthode principale pour tester depuis la ligne de commande.
     */
    public static void main(String[] args) {
        System.out.println("🚀 Démarrage du test de notifications...");
        
        // Initialiser JavaFX Platform
        Platform.startup(() -> {
            testBothNotifications();
        });
        
        // Garder le programme en vie
        try {
            Thread.sleep(15000); // 15 secondes
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Platform.exit();
        System.out.println("✅ Test terminé");
    }
}
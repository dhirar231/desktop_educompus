package com.educompus.debug;

import com.educompus.model.NotificationType;
import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;
import com.educompus.service.NotificationService;
import javafx.application.Platform;

import java.time.LocalDateTime;

/**
 * Test ultra-rapide pour la notification urgente.
 * Lance directement la notification sans interface.
 */
public class QuickUrgentTest {
    
    public static void main(String[] args) {
        System.out.println("🚨 TEST RAPIDE - NOTIFICATION URGENTE");
        System.out.println("=====================================");
        
        // Initialiser JavaFX Platform
        Platform.startup(() -> {
            System.out.println("⚡ Envoi de la notification urgente dans 2 secondes...");
            
            // Attendre 2 secondes puis envoyer
            Platform.runLater(() -> {
                try {
                    Thread.sleep(2000);
                    sendUrgentNotification();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });
        
        // Garder le programme en vie pour voir la notification
        try {
            Thread.sleep(30000); // 30 secondes
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("✅ Test terminé");
        Platform.exit();
        System.exit(0);
    }
    
    private static void sendUrgentNotification() {
        System.out.println("🚨 DÉCLENCHEMENT DE LA NOTIFICATION URGENTE...");
        
        // Créer une session de test
        SessionLive session = createTestSession();
        
        // Envoyer la notification urgente
        NotificationService service = new NotificationService();
        service.sendUrgentNotification(session, NotificationType.FIVE_MINUTES);
        
        System.out.println("✅ Notification urgente envoyée !");
        System.out.println("📋 Vous devriez voir un popup rouge avec :");
        System.out.println("   • Titre: '⚠️ Session dans 5 minutes !'");
        System.out.println("   • Bouton 'Rejoindre maintenant' (ouvre le lien)");
        System.out.println("   • Bouton 'Plus tard' (ferme le popup)");
        System.out.println("   • Style rouge/urgent");
        System.out.println("   • Reste ouvert jusqu'à action utilisateur");
    }
    
    private static SessionLive createTestSession() {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime heureSession = maintenant.plusMinutes(5);
        
        SessionLive session = new SessionLive();
        session.setId(999);
        session.setNomCours("Session Test Urgente");
        session.setTitre("Session Test Urgente");
        session.setCoursTitre("Cours de Test");
        session.setEnseignantNom("Prof. Test");
        session.setLien("https://meet.google.com/test-urgent");
        session.setLienSession("https://meet.google.com/test-urgent");
        session.setDate(heureSession.toLocalDate());
        session.setHeure(heureSession.toLocalTime());
        session.setDateDebut(heureSession);
        session.setDateFin(heureSession.plusHours(1));
        session.setStatut(SessionStatut.PLANIFIEE);
        
        return session;
    }
}
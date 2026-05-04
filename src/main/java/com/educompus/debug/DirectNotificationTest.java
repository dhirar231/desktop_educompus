package com.educompus.debug;

import com.educompus.model.NotificationType;
import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.time.LocalDateTime;

/**
 * Test direct de notification sans hériter d'Application.
 * Utilise directement les Alert JavaFX.
 */
public class DirectNotificationTest {
    
    public static void main(String[] args) {
        System.out.println("🚨 TEST DIRECT DE NOTIFICATION URGENTE");
        System.out.println("======================================");
        
        // Initialiser JavaFX Platform
        Platform.startup(() -> {
            System.out.println("✅ JavaFX Platform initialisé");
            
            // Attendre 2 secondes puis lancer le test
            Platform.runLater(() -> {
                try {
                    Thread.sleep(2000);
                    System.out.println("🚨 Lancement du test...");
                    testUrgentNotificationDirect();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });
        
        // Garder le programme en vie
        try {
            Thread.sleep(30000); // 30 secondes
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Platform.exit();
        System.out.println("✅ Test terminé");
    }
    
    private static void testUrgentNotificationDirect() {
        Platform.runLater(() -> {
            try {
                System.out.println("🚨 CRÉATION DE LA NOTIFICATION URGENTE...");
                
                // Créer une session de test
                SessionLive session = createTestSession();
                
                // Créer l'Alert urgente manuellement
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Session Live - URGENT");
                alert.setHeaderText("⚠️ Session dans 5 minutes !");
                
                // Message complet
                String message = buildUrgentMessage(session);
                alert.setContentText(message);
                
                // Boutons personnalisés
                ButtonType joinButton = new ButtonType("🔗 Rejoindre maintenant");
                ButtonType laterButton = new ButtonType("Plus tard");
                alert.getButtonTypes().setAll(joinButton, laterButton);
                
                // Appliquer le style urgent (si possible)
                try {
                    alert.getDialogPane().getStylesheets().add(
                        DirectNotificationTest.class.getResource("/styles/educompus.css").toExternalForm()
                    );
                    alert.getDialogPane().getStyleClass().add("notification-urgent");
                } catch (Exception e) {
                    System.out.println("⚠️ Impossible d'appliquer les styles CSS: " + e.getMessage());
                }
                
                System.out.println("✅ NOTIFICATION URGENTE AFFICHÉE !");
                System.out.println("👀 Vous devriez voir :");
                System.out.println("   • Popup avec titre '⚠️ Session dans 5 minutes !'");
                System.out.println("   • Bouton '🔗 Rejoindre maintenant'");
                System.out.println("   • Bouton 'Plus tard'");
                System.out.println("   • Informations de la session");
                
                // Afficher et gérer la réponse
                alert.showAndWait().ifPresent(response -> {
                    if (response == joinButton) {
                        System.out.println("🔗 Utilisateur a cliqué 'Rejoindre maintenant'");
                        openSessionLink(session.getLienSession());
                    } else {
                        System.out.println("⏰ Utilisateur a cliqué 'Plus tard'");
                    }
                });
                
            } catch (Exception e) {
                System.err.println("❌ ERREUR lors du test: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private static SessionLive createTestSession() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionTime = now.plusMinutes(5);
        
        SessionLive session = new SessionLive();
        session.setId(999);
        session.setNomCours("COURS TEST URGENT");
        session.setTitre("COURS TEST URGENT");
        session.setCoursTitre("Mathématiques Avancées");
        session.setEnseignantNom("Prof. Martin Dubois");
        session.setLien("https://meet.google.com/test-urgent-session");
        session.setLienSession("https://meet.google.com/test-urgent-session");
        session.setDate(sessionTime.toLocalDate());
        session.setHeure(sessionTime.toLocalTime());
        session.setDateDebut(sessionTime);
        session.setDateFin(sessionTime.plusHours(1));
        session.setStatut(SessionStatut.PLANIFIEE);
        
        return session;
    }
    
    private static String buildUrgentMessage(SessionLive session) {
        StringBuilder message = new StringBuilder();
        message.append("📚 Cours: ").append(session.getCoursTitre()).append("\n");
        message.append("📖 Titre: ").append(session.getTitre()).append("\n");
        message.append("👨‍🏫 Enseignant: ").append(session.getEnseignantNom()).append("\n");
        message.append("🕐 Heure: ").append(session.getDateHeureFormatee()).append("\n\n");
        message.append("⚠️ La session commence bientôt !\n");
        message.append("Cliquez sur 'Rejoindre maintenant' pour accéder directement à la session.");
        
        return message.toString();
    }
    
    private static void openSessionLink(String link) {
        try {
            System.out.println("🔗 Tentative d'ouverture du lien: " + link);
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(link));
            System.out.println("✅ Lien ouvert dans le navigateur");
        } catch (Exception e) {
            System.err.println("❌ Impossible d'ouvrir le lien: " + e.getMessage());
            System.out.println("📋 Lien copié dans le presse-papier (simulation)");
        }
    }
}
package com.educompus.debug;

import com.educompus.model.NotificationType;
import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;
import com.educompus.service.NotificationService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;

/**
 * Test simple et direct pour la notification urgente.
 * Fonctionne sans configuration Maven complexe.
 */
public class SimpleNotificationTest extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        System.out.println("🚨 APPLICATION DE TEST DÉMARRÉE");
        
        VBox root = new VBox(20);
        root.setStyle("-fx-padding: 30; -fx-alignment: center;");
        
        Label title = new Label("🚨 TEST NOTIFICATION URGENTE");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: red;");
        
        Button testBtn = new Button("DÉCLENCHER NOTIFICATION URGENTE");
        testBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-color: red; -fx-text-fill: white;");
        testBtn.setOnAction(e -> {
            System.out.println("🚨 DÉCLENCHEMENT...");
            testUrgentNotification();
        });
        
        Label info = new Label("Cliquez pour voir la notification rouge avec boutons");
        info.setStyle("-fx-font-size: 12px;");
        
        root.getChildren().addAll(title, testBtn, info);
        
        Scene scene = new Scene(root, 400, 200);
        primaryStage.setTitle("Test Notification");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Test automatique après 3 secondes
        Platform.runLater(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("🚨 TEST AUTOMATIQUE DANS 3 SECONDES...");
                testUrgentNotification();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private void testUrgentNotification() {
        try {
            System.out.println("🚨 Création de la session de test...");
            
            // Session de test
            SessionLive session = new SessionLive();
            session.setId(999);
            session.setNomCours("COURS TEST URGENT");
            session.setTitre("COURS TEST URGENT");
            session.setCoursTitre("Mathématiques");
            session.setEnseignantNom("Prof. Martin");
            session.setLien("https://meet.google.com/test-urgent-session");
            session.setLienSession("https://meet.google.com/test-urgent-session");
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime sessionTime = now.plusMinutes(5);
            session.setDate(sessionTime.toLocalDate());
            session.setHeure(sessionTime.toLocalTime());
            session.setDateDebut(sessionTime);
            session.setDateFin(sessionTime.plusHours(1));
            session.setStatut(SessionStatut.PLANIFIEE);
            
            System.out.println("📋 Session créée: " + session.getTitre());
            System.out.println("🕐 Heure: " + session.getDateHeureFormatee());
            System.out.println("🔗 Lien: " + session.getLienSession());
            
            // Envoyer la notification
            System.out.println("🚨 ENVOI DE LA NOTIFICATION URGENTE...");
            NotificationService service = new NotificationService();
            service.sendUrgentNotification(session, NotificationType.FIVE_MINUTES);
            
            System.out.println("✅ NOTIFICATION ENVOYÉE !");
            System.out.println("👀 Vous devriez voir un popup rouge avec :");
            System.out.println("   • Titre: '⚠️ Session dans 5 minutes !'");
            System.out.println("   • Bouton 'Rejoindre maintenant'");
            System.out.println("   • Bouton 'Plus tard'");
            System.out.println("   • Style rouge/urgent");
            
        } catch (Exception e) {
            System.err.println("❌ ERREUR: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback avec Alert simple
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur Test");
                alert.setHeaderText("Erreur lors du test");
                alert.setContentText("Erreur: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }
    
    public static void main(String[] args) {
        System.out.println("🚀 LANCEMENT DU TEST DE NOTIFICATION URGENTE");
        System.out.println("============================================");
        System.out.println("📋 Ce test va montrer la notification rouge avec boutons");
        System.out.println("⏰ Test automatique dans 3 secondes après ouverture");
        
        launch(args);
    }
}
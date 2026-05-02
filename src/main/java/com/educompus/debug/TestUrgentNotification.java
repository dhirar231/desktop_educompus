package com.educompus.debug;

import com.educompus.model.NotificationType;
import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;
import com.educompus.service.NotificationService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;

/**
 * Test spécifique pour la notification urgente (5 minutes).
 */
public class TestUrgentNotification extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(30);
        root.setStyle("-fx-padding: 40; -fx-alignment: center; -fx-background-color: #f5f5f5;");
        
        Label title = new Label("⚠️ Test Notification Urgente");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #d6293e;");
        
        Label description = new Label("Cliquez pour voir la notification urgente (5 minutes)");
        description.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        
        Button testBtn = new Button("🚨 DÉCLENCHER NOTIFICATION URGENTE");
        testBtn.setStyle("""
            -fx-font-size: 16px; 
            -fx-font-weight: bold;
            -fx-padding: 15 30; 
            -fx-background-color: #d6293e; 
            -fx-text-fill: white;
            -fx-background-radius: 8px;
            -fx-cursor: hand;
        """);
        
        testBtn.setOnAction(e -> {
            System.out.println("🚨 Déclenchement de la notification urgente...");
            testUrgentNotification();
        });
        
        Label info = new Label("""
            La notification urgente devrait apparaître avec :
            • Popup rouge avec bordure rouge
            • Icône ⚠️ et titre "Session dans 5 minutes !"
            • Bouton "Rejoindre maintenant" (ouvre le lien)
            • Bouton "Plus tard" (ferme la notification)
            • Reste ouverte jusqu'à action utilisateur
        """);
        info.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-text-alignment: left;");
        
        root.getChildren().addAll(title, description, testBtn, info);
        
        Scene scene = new Scene(root, 500, 400);
        primaryStage.setTitle("Test Notification Urgente - EduCompus");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        System.out.println("✅ Interface de test notification urgente prête");
        System.out.println("📋 Cliquez sur le bouton pour déclencher la notification");
    }
    
    private void testUrgentNotification() {
        // Créer une session de test réaliste
        SessionLive session = new SessionLive();
        session.setId(999);
        session.setNomCours("Mathématiques Avancées");
        session.setTitre("Cours sur les Intégrales");
        session.setCoursTitre("Mathématiques Avancées");
        session.setEnseignantNom("Prof. Martin Dubois");
        session.setLien("https://meet.google.com/abc-defg-hij");
        session.setLienSession("https://meet.google.com/abc-defg-hij");
        
        // Session dans 5 minutes
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime heureSession = maintenant.plusMinutes(5);
        session.setDate(heureSession.toLocalDate());
        session.setHeure(heureSession.toLocalTime());
        session.setDateDebut(heureSession);
        session.setDateFin(heureSession.plusHours(1));
        session.setStatut(SessionStatut.PLANIFIEE);
        
        // Envoyer la notification
        NotificationService service = new NotificationService();
        service.sendUrgentNotification(session, NotificationType.FIVE_MINUTES);
        
        System.out.println("🚨 Notification urgente envoyée !");
        System.out.println("📋 Session: " + session.getTitre());
        System.out.println("👨‍🏫 Enseignant: " + session.getEnseignantNom());
        System.out.println("🕐 Heure: " + session.getDateHeureFormatee());
        System.out.println("🔗 Lien: " + session.getLienSession());
    }
    
    public static void main(String[] args) {
        System.out.println("🚨 Lancement du test de notification urgente...");
        System.out.println("⚠️ Cette notification simule une session qui commence dans 5 minutes");
        launch(args);
    }
}
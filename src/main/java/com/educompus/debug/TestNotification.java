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
 * Application de test pour les notifications.
 * Lance une fenêtre avec des boutons pour tester les notifications.
 */
public class TestNotification extends Application {
    
    private NotificationService notificationService;
    
    @Override
    public void start(Stage primaryStage) {
        notificationService = new NotificationService();
        
        // Interface de test
        VBox root = new VBox(20);
        root.setStyle("-fx-padding: 20; -fx-alignment: center;");
        
        Label title = new Label("🧪 Test des Notifications EduCompus");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Button testInfoBtn = new Button("📅 Tester Notification Info (30min)");
        testInfoBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");
        testInfoBtn.setOnAction(e -> testInfoNotification());
        
        Button testUrgentBtn = new Button("⚠️ Tester Notification Urgente (5min)");
        testUrgentBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");
        testUrgentBtn.setOnAction(e -> testUrgentNotification());
        
        Button testBothBtn = new Button("🚀 Tester les Deux Types");
        testBothBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");
        testBothBtn.setOnAction(e -> testBothNotifications());
        
        Label info = new Label("Cliquez sur un bouton pour tester les notifications");
        info.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        
        root.getChildren().addAll(title, testInfoBtn, testUrgentBtn, testBothBtn, info);
        
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("Test Notifications EduCompus");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        System.out.println("✅ Interface de test des notifications lancée");
    }
    
    private void testInfoNotification() {
        SessionLive session = createTestSession("Session Test Info", 30);
        notificationService.sendInfoNotification(session, NotificationType.THIRTY_MINUTES);
        System.out.println("📅 Notification info envoyée");
    }
    
    private void testUrgentNotification() {
        SessionLive session = createTestSession("Session Test Urgente", 5);
        notificationService.sendUrgentNotification(session, NotificationType.FIVE_MINUTES);
        System.out.println("⚠️ Notification urgente envoyée");
    }
    
    private void testBothNotifications() {
        System.out.println("🚀 Test des deux types de notifications...");
        
        testInfoNotification();
        
        // Attendre 3 secondes puis tester la notification urgente
        Platform.runLater(() -> {
            try {
                Thread.sleep(3000);
                testUrgentNotification();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private SessionLive createTestSession(String nom, int minutesAvant) {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime heureSession = maintenant.plusMinutes(minutesAvant);
        
        SessionLive session = new SessionLive();
        session.setId(999);
        session.setNomCours(nom);
        session.setTitre(nom);
        session.setCoursTitre("Cours de Mathématiques");
        session.setEnseignantNom("Prof. Dupont");
        session.setLien("https://meet.google.com/abc-defg-hij");
        session.setLienSession("https://meet.google.com/abc-defg-hij");
        session.setDate(heureSession.toLocalDate());
        session.setHeure(heureSession.toLocalTime());
        session.setDateDebut(heureSession);
        session.setDateFin(heureSession.plusHours(1));
        session.setStatut(SessionStatut.PLANIFIEE);
        
        return session;
    }
    
    public static void main(String[] args) {
        System.out.println("🧪 Lancement du test des notifications...");
        launch(args);
    }
}
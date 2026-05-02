package com.educompus.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.educompus.controller.front.SplashController;
import com.educompus.nav.Navigator;
import com.educompus.service.SessionNotificationService;
import com.educompus.service.NotificationSchedulerService;
import com.educompus.service.NotificationService;
import com.educompus.repository.NotificationRepository;
import com.educompus.repository.SessionLiveRepository;
import com.educompus.util.DatabaseSetup;
import com.educompus.util.Theme;
import com.educompus.util.NotificationTester;
import com.educompus.debug.TestNotificationInApp;

import java.io.File;

public final class PreviewApp extends Application {
    
    private NotificationSchedulerService notificationScheduler;
    
    @Override
    public void start(Stage stage) throws Exception {
        // Configurer la base de données au démarrage
        DatabaseSetup.ensureTablesExist();

        // Start embedded HTTP server so mobile phones can open exams directly
        try {
            com.educompus.web.EmbeddedExamServer server = new com.educompus.web.EmbeddedExamServer(8000);
            server.start();
            System.out.println("EmbeddedExamServer started on port 8000");
        } catch (Exception e) {
            System.err.println("Could not start EmbeddedExamServer: " + e.getMessage());
        }

        String fxmlPath = System.getProperty("fxml", "View/front/FrontLogin.fxml");
        String cssPath = System.getProperty("css", "styles/educompus.css");
        String title = System.getProperty("title", "EduCampus");
        boolean splashEnabled = !"false".equalsIgnoreCase(System.getProperty("splash", "true").trim());

        String theme = System.getProperty("theme", "light").trim().toLowerCase();
        AppState.setDark("dark".equals(theme));

        if (!splashEnabled) {
            Navigator.init(stage, cssPath);
            Navigator.goRoot(fxmlPath);

            Parent root = stage.getScene().getRoot();
            Theme.apply(root);

            stage.setTitle(title);
            stage.setMinWidth(1040);
            stage.setMinHeight(680);
            stage.show();
            
            startNotificationScheduler();
            
            return;
        }

        // Splash (transparent)
        stage.initStyle(StageStyle.TRANSPARENT);

        FXMLLoader splashLoader = Navigator.loader(System.getProperty("splashFxml", "View/front/Splash.fxml"));
        Parent splashRoot = splashLoader.load();
        Scene splashScene = new Scene(splashRoot, 520, 420);
        splashScene.setFill(Color.TRANSPARENT);

        File cssFile = Navigator.resolvePath(cssPath);
        if (cssFile != null && cssFile.exists()) {
            splashScene.getStylesheets().add(cssFile.toURI().toString());
        }
        Theme.apply(splashRoot);

        stage.setTitle(title);
        stage.setScene(splashScene);
        stage.centerOnScreen();
        stage.show();

        SplashController controller = splashLoader.getController();
        if (controller != null) {
            controller.setOnDone(() -> {
                Stage mainStage = new Stage();
                try {
                    Navigator.init(mainStage, cssPath);
                    Navigator.goRoot(fxmlPath);
                    Parent root = mainStage.getScene().getRoot();
                    Theme.apply(root);

                    mainStage.setTitle(title);
                    mainStage.setMinWidth(1040);
                    mainStage.setMinHeight(680);
                    mainStage.show();

                    SessionNotificationService.getInstance().demarrer(mainStage);
                    startNotificationScheduler();
                } finally {
                    stage.close();
                }
            });
            controller.play();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    private void startNotificationScheduler() {
        try {
            NotificationService notificationService = new NotificationService();
            SessionLiveRepository sessionRepository = new SessionLiveRepository();
            NotificationRepository notificationRepository = new NotificationRepository();
            
            notificationScheduler = new NotificationSchedulerService(
                notificationService, 
                sessionRepository, 
                notificationRepository
            );
            
            notificationScheduler.start();
            System.out.println("[NotificationScheduler] Nouveau système de notifications démarré");
            
            if (Boolean.parseBoolean(System.getProperty("test.notifications", "false"))) {
                System.out.println("🧪 Mode test activé - Envoi de notifications de test...");
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(3000);
                        NotificationTester.testBothNotifications();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            System.out.println("🚨 DÉCLENCHEMENT AUTOMATIQUE DU TEST DE NOTIFICATION URGENTE DANS 5 SECONDES...");
            Platform.runLater(() -> {
                try {
                    Thread.sleep(5000);
                    System.out.println("🚨 LANCEMENT DU TEST NOTIFICATION URGENTE...");
                    TestNotificationInApp.testUrgentNotificationNow();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
        } catch (Exception e) {
            System.err.println("[NotificationScheduler] Erreur lors du démarrage: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void stop() throws Exception {
        try {
            if (notificationScheduler != null) {
                notificationScheduler.stop();
                System.out.println("[NotificationScheduler] Système de notifications arrêté");
            }
        } catch (Exception e) {
            System.err.println("[NotificationScheduler] Erreur lors de l'arrêt: " + e.getMessage());
        }
        
        super.stop();
    }
}
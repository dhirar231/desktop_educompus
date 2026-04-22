package com.educompus.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.educompus.controller.front.SplashController;
import com.educompus.nav.Navigator;
import com.educompus.util.DatabaseSetup;
import com.educompus.util.Theme;

import java.io.File;

public final class PreviewApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Configurer la base de données au démarrage
        DatabaseSetup.ensureTablesExist();
        
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
}

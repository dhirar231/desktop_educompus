package com.educompus.app;

import com.educompus.app.AppState;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import java.io.File;

public final class DebugFxmlLoad {
    public static void main(String[] args) throws Exception {
        Platform.startup(() -> {
        });

        try {
            AppState.setRole(AppState.Role.ADMIN);

            File fxml = new File("View/back/BackShell.fxml");
            if (!fxml.exists()) {
                fxml = new File("eduCompus-javafx/src/main/resources/View/back/BackShell.fxml");
            }
            System.out.println("Loading: " + fxml.getAbsolutePath());
            FXMLLoader loader = new FXMLLoader(fxml.toURI().toURL());
            loader.load();
            System.out.println("FXML OK");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Platform.exit();
        }
    }
}

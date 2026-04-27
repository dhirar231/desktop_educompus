package com.educompus.util;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Window;
import javafx.stage.Stage;

public final class Dialogs {
    private Dialogs() {}

    public static void info(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        style(alert);
        alert.showAndWait();
    }

    public static void warning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        style(alert);
        alert.showAndWait();
    }

    public static void error(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        style(alert);
        alert.showAndWait();
    }

    public static void style(Dialog<?> d) {
        if (d == null || d.getDialogPane() == null) return;
        String css = cssUri();
        if (!css.isBlank() && !d.getDialogPane().getStylesheets().contains(css)) {
            d.getDialogPane().getStylesheets().add(css);
        }
        if (!d.getDialogPane().getStyleClass().contains("rgb-dialog")) {
            d.getDialogPane().getStyleClass().add("rgb-dialog");
        }
        for (ButtonType bt : d.getDialogPane().getButtonTypes()) {
            Node b = d.getDialogPane().lookupButton(bt);
            if (b == null) continue;
            if (bt == ButtonType.OK || bt == ButtonType.YES) {
                b.getStyleClass().add("btn-rgb");
            } else if (bt == ButtonType.CANCEL || bt == ButtonType.NO) {
                b.getStyleClass().add("btn-rgb-outline");
            }
        }

        // Try to set application icon on the dialog's window if possible
        try {
            Window w = d.getDialogPane().getScene().getWindow();
            if (w instanceof Stage) {
                Stage s = (Stage) w;
                java.net.URL ico = Dialogs.class.getResource("/assets/images/app-icon.png");
                if (ico == null) {
                    ico = Dialogs.class.getResource("/assets/images/app-icon.ico");
                }
                if (ico != null) {
                    try {
                        Image img = new Image(ico.toExternalForm());
                        if (!s.getIcons().contains(img)) s.getIcons().add(img);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    private static String cssUri() {
        java.io.File f = new java.io.File("styles/educompus.css");
        if (!f.exists()) {
            f = new java.io.File("eduCompus-javafx/styles/educompus.css");
        }
        if (!f.exists()) {
            f = new java.io.File(new java.io.File("..", "eduCompus-javafx"), "styles/educompus.css");
        }
        return f.exists() ? f.toURI().toString() : "";
    }
}

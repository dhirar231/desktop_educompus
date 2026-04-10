package com.educompus.util;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public final class Dialogs {
    private Dialogs() {}

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
            if (bt == ButtonType.OK) {
                b.getStyleClass().add("btn-rgb");
            } else if (bt == ButtonType.CANCEL) {
                b.getStyleClass().add("btn-rgb-outline");
            }
        }
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

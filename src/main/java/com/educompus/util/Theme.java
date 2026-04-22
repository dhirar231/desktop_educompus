package com.educompus.util;

import com.educompus.app.AppState;

import javafx.scene.Parent;
import javafx.scene.paint.Color;

public final class Theme {
    private static Color primary = Color.web("#066ac9");

    private Theme() {
    }

    public static Color getPrimary() {
        return primary;
    }

    public static void setPrimary(Color value) {
        primary = value == null ? Color.web("#066ac9") : value;
    }

    public static void apply(Parent root) {
        if (root == null) {
            return;
        }

        boolean dark = AppState.isDark();
        Color faintFocus = new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 0.18);

        String style = "-edu-primary: " + toCss(primary) + ";" +
                "-edu-primary-contrast: " + (dark ? "#0b0f13" : "#ffffff") + ";" +
                "-edu-accent: " + (dark ? "#9bd0ff" : "#0b2942") + ";" +
                "-edu-bg: " + (dark ? "#0b1220" : "#ffffff") + ";" +
                "-edu-surface: " + (dark ? "#111a2c" : "#f8fafc") + ";" +
                "-edu-surface-2: " + (dark ? "#0f172a" : "#f1f5f9") + ";" +
                "-edu-sidebar: " + (dark ? "#0b1220" : "#ffffff") + ";" +
                "-edu-card: " + (dark ? "#111a2c" : "#ffffff") + ";" +
                "-edu-border: " + (dark ? "#253047" : "#e2e8f0") + ";" +
                "-edu-text: " + (dark ? "#e5e7eb" : "#0f172a") + ";" +
                "-edu-text-muted: " + (dark ? "rgba(203,213,225,0.75)" : "rgba(71,85,105,0.85)") + ";" +
                "-fx-accent: " + toCss(primary) + ";" +
                "-fx-focus-color: " + toCss(primary) + ";" +
                "-fx-faint-focus-color: " + toCss(faintFocus) + ";";

        root.setStyle(style);
    }

    private static String toCss(Color c) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        double a = c.getOpacity();
        if (a >= 0.999) {
            return String.format("rgb(%d,%d,%d)", r, g, b);
        }
        return String.format("rgba(%d,%d,%d,%.3f)", r, g, b, a);
    }
}

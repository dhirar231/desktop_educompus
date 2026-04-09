package com.educompus.util;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public final class AvatarGenerator {
    private AvatarGenerator() {
    }

    public static Image generate(String seed, int size) {
        int s = Math.max(24, Math.min(size, 256));
        String txt = initials(seed);
        Color bg = pickColor(seed);

        Canvas canvas = new Canvas(s, s);
        GraphicsContext g = canvas.getGraphicsContext2D();

        g.setFill(bg);
        g.fillOval(0, 0, s, s);

        g.setFill(Color.rgb(255, 255, 255, 0.92));
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, Math.max(12, s * 0.38)));
        g.fillText(txt, s / 2.0, s / 2.0 + (s * 0.14));

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage out = new WritableImage(s, s);
        return canvas.snapshot(params, out);
    }

    private static String initials(String seed) {
        String s = seed == null ? "" : seed.trim();
        if (s.isBlank()) {
            return "U";
        }
        int at = s.indexOf('@');
        if (at > 0) {
            s = s.substring(0, at);
        }
        String[] parts = s.replaceAll("[^A-Za-z0-9 ]", " ").trim().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            return "U";
        }
        String a = parts[0].substring(0, 1).toUpperCase();
        String b = parts.length > 1 ? parts[1].substring(0, 1).toUpperCase() : "";
        return (a + b).trim();
    }

    private static Color pickColor(String seed) {
        int h = (seed == null ? 0 : seed.hashCode());
        double hue = (Math.abs(h) % 360);
        return Color.hsb(hue, 0.70, 0.78);
    }
}

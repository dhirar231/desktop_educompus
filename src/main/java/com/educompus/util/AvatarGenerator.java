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

    public static Image generateTeacher(int size) {
        int s = Math.max(24, Math.min(size, 256));
        Canvas canvas = new Canvas(s, s);
        GraphicsContext g = canvas.getGraphicsContext2D();

        // Background circle using primary template blue
        Color bg = Color.web("#066ac9");
        g.setFill(bg);
        g.fillOval(0, 0, s, s);

        // Draw mortarboard (simple stylized hat)
        g.setFill(Color.WHITE);
        double cx = s / 2.0;
        double topY = s * 0.28;
        double hatW = s * 0.62;
        double hatH = s * 0.18;

        // Diamond/flat top
        double[] xPoints = {cx - hatW / 2, cx, cx + hatW / 2, cx};
        double[] yPoints = {topY + hatH / 2, topY - hatH / 2, topY + hatH / 2, topY + hatH};
        g.fillPolygon(xPoints, yPoints, xPoints.length);

        // Small square under the hat (cap)
        double capW = hatW * 0.28;
        double capH = hatH * 0.85;
        double capX = cx - capW / 2;
        double capY = topY + hatH / 2;
        g.fillRect(capX, capY, capW, capH);

        // Tassel (thin line + small circle)
        g.setStroke(Color.web("#ffd54a"));
        g.setLineWidth(Math.max(1, s * 0.04));
        double tasselStartX = cx + capW / 2;
        double tasselStartY = capY + capH * 0.25;
        g.strokeLine(tasselStartX, tasselStartY, tasselStartX + s * 0.06, tasselStartY + s * 0.26);
        g.setFill(Color.web("#ffd54a"));
        g.fillOval(tasselStartX + s * 0.06 - s * 0.03, tasselStartY + s * 0.26 - s * 0.03, s * 0.06, s * 0.06);

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

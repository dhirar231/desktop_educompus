package com.educompus.util;

import java.io.File;
import java.util.List;

public final class ProjectRules {
    private ProjectRules() {
    }

    public static String validateSubmissionFields(String response, String cahierPath, String dossierPath) {
        String r = safe(response);
        if (r.isBlank()) {
            return "Réponse requise (texte + chiffres).";
        }
        if (!isSubmissionResponseValid(r)) {
            return "Réponse invalide (autorisé: lettres, chiffres, espaces).";
        }

        String c = safe(cahierPath);
        if (c.isBlank()) {
            return "Fichier cahier requis (pdf/doc/pptx/image/... ).";
        }
        if (!isFilePathValid(c, List.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "jpg", "jpeg", "png"))) {
            return "Cahier invalide (type/chemin). Formats acceptés: pdf, doc, docx, ppt, pptx, xls, xlsx, jpg, jpeg, png.";
        }

        String d = safe(dossierPath);
        if (d.isBlank()) {
            return "Fichier dossier requis (.zip/.rar/.7z).";
        }
        if (!isFilePathValid(d, List.of("zip", "rar", "7z"))) {
            return "Dossier invalide (type/chemin).";
        }
        return null;
    }

    public static boolean isSubmissionResponseValid(String text) {
        String v = safe(text);
        if (v.length() < 2) {
            return false;
        }
        return v.matches("[\\p{L}\\p{N}\\s\\.,;:!\\?\\(\\)\\[\\]\\{\\}'\"\\-_/\\\\@#]+");
    }

    public static boolean isKanbanTitleValid(String text) {
        String v = safe(text);
        if (v.length() < 2 || v.length() > 16) {
            return false;
        }
        return v.matches("^[\\p{L}][\\p{L}\\s'\\-]{1,15}$");
    }

    public static String kanbanTitleValidationError(String text) {
        String v = safe(text);
        if (v.isBlank()) {
            return "Titre requis (2-16 lettres).";
        }
        if (v.length() < 2) {
            return "Titre trop court (min 2 lettres).";
        }
        if (v.length() > 16) {
            return "Titre trop long (max 16 caractères).";
        }
        if (!Character.isLetter(v.codePointAt(0))) {
            return "Le titre doit commencer par une lettre.";
        }
        if (!v.matches("^[\\p{L}][\\p{L}\\s'\\-]{1,15}$")) {
            return "Caractères invalides: utilisez seulement lettres, espaces, apostrophe ou tiret.";
        }
        return null;
    }

    public static boolean isFilePathValid(String path, List<String> allowedExt) {
        String p = safe(path);
        if (p.isBlank()) {
            return false;
        }
        File f = new File(p);
        if (!f.exists() || !f.isFile()) {
            return false;
        }
        String ext = extensionOf(f.getName());
        if (ext.isBlank()) {
            return false;
        }
        for (String a : allowedExt) {
            if (ext.equalsIgnoreCase(safe(a))) {
                return true;
            }
        }
        return false;
    }

    public static String extensionOf(String filename) {
        String v = safe(filename);
        int idx = v.lastIndexOf('.');
        if (idx <= 0 || idx >= v.length() - 1) {
            return "";
        }
        String ext = v.substring(idx + 1).trim().toLowerCase();
        if (ext.length() > 12) {
            return "";
        }
        if (!ext.matches("[a-z0-9]+")) {
            return "";
        }
        return ext;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}

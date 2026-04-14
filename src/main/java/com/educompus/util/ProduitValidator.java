package com.educompus.util;

import javafx.scene.control.*;
import javafx.scene.layout.Region;

/**
 * Validation en temps réel des champs du formulaire produit.
 * Chaque règle est attachée via un listener sur focusedProperty (on blur)
 * et textProperty (on type) pour un feedback immédiat.
 */
public final class ProduitValidator {

    private ProduitValidator() {}

    // ── Attache tous les listeners ────────────────────────────────────────────

    public static void attacher(
            TextField fieldNom,         Label errNom,
            TextArea  fieldDescription, Label errDescription,
            TextField fieldPrix,        Label errPrix,
            TextField fieldStock,       Label errStock,
            ComboBox<String> fieldType, Label errType,
            ComboBox<String> fieldCategorie, Label errCategorie,
            TextField fieldImage,       Label errImage
    ) {
        // Nom : obligatoire, 3–100 caractères, pas de chiffres seuls
        attachTexte(fieldNom, errNom, val -> {
            if (val.isBlank())          return "Le nom est obligatoire.";
            if (val.length() < 3)       return "Le nom doit contenir au moins 3 caractères.";
            if (val.length() > 100)     return "Le nom ne peut pas dépasser 100 caractères.";
            if (val.matches("\\d+"))    return "Le nom ne peut pas être uniquement des chiffres.";
            return null;
        });

        // Description : obligatoire, min 10 caractères
        attachTextArea(fieldDescription, errDescription, val -> {
            if (val.isBlank())      return "La description est obligatoire.";
            if (val.length() < 10)  return "La description doit contenir au moins 10 caractères.";
            if (val.length() > 1000) return "La description ne peut pas dépasser 1000 caractères.";
            return null;
        });

        // Prix : nombre décimal positif, max 99999.99
        attachTexte(fieldPrix, errPrix, val -> {
            if (val.isBlank()) return "Le prix est obligatoire.";
            try {
                double v = Double.parseDouble(val.replace(",", "."));
                if (v < 0)       return "Le prix doit être positif ou nul.";
                if (v > 99999.99) return "Le prix ne peut pas dépasser 99 999,99 TND.";
            } catch (NumberFormatException e) {
                return "Prix invalide — entrez un nombre (ex: 29.99).";
            }
            return null;
        });

        // Stock : entier ≥ 0, max 99999
        attachTexte(fieldStock, errStock, val -> {
            if (val.isBlank()) return "Le stock est obligatoire.";
            try {
                int v = Integer.parseInt(val.trim());
                if (v < 0)     return "Le stock ne peut pas être négatif.";
                if (v > 99999) return "Le stock ne peut pas dépasser 99 999.";
            } catch (NumberFormatException e) {
                return "Stock invalide — entrez un nombre entier (ex: 50).";
            }
            return null;
        });

        // Type : sélection obligatoire
        attachCombo(fieldType, errType, val ->
                val == null ? "Veuillez sélectionner un type." : null);

        // Catégorie : sélection obligatoire
        attachCombo(fieldCategorie, errCategorie, val ->
                val == null ? "Veuillez sélectionner une catégorie." : null);

        // Image : optionnel, mais si renseigné doit être URL ou chemin valide
        attachTexte(fieldImage, errImage, val -> {
            if (val.isBlank()) return null; // optionnel
            boolean isUrl  = val.startsWith("http://") || val.startsWith("https://")
                          || val.startsWith("file:/");
            boolean isPath = val.contains("/") || val.contains("\\");
            if (!isUrl && !isPath)
                return "Format invalide — entrez une URL (https://…) ou un chemin de fichier.";
            if (!val.matches(".*\\.(png|jpg|jpeg|gif|webp|PNG|JPG|JPEG|GIF|WEBP).*"))
                return "Extension non reconnue — utilisez png, jpg, jpeg, gif ou webp.";
            return null;
        });
    }

    // ── Validation globale avant soumission ───────────────────────────────────

    /**
     * Déclenche toutes les validations et retourne true si tout est valide.
     * Appeler juste avant onEnregistrer / onValider.
     */
    public static boolean validerTout(
            TextField fieldNom,         Label errNom,
            TextArea  fieldDescription, Label errDescription,
            TextField fieldPrix,        Label errPrix,
            TextField fieldStock,       Label errStock,
            ComboBox<String> fieldType, Label errType,
            ComboBox<String> fieldCategorie, Label errCategorie,
            TextField fieldImage,       Label errImage
    ) {
        boolean ok = true;
        ok &= validerTexte(fieldNom.getText().trim(),        errNom,         fieldNom,         v -> {
            if (v.isBlank())       return "Le nom est obligatoire.";
            if (v.length() < 3)    return "Le nom doit contenir au moins 3 caractères.";
            if (v.length() > 100)  return "Le nom ne peut pas dépasser 100 caractères.";
            if (v.matches("\\d+")) return "Le nom ne peut pas être uniquement des chiffres.";
            return null;
        });
        ok &= validerTexte(fieldDescription.getText().trim(), errDescription, fieldDescription, v -> {
            if (v.isBlank())       return "La description est obligatoire.";
            if (v.length() < 10)   return "La description doit contenir au moins 10 caractères.";
            if (v.length() > 1000) return "La description ne peut pas dépasser 1000 caractères.";
            return null;
        });
        ok &= validerTexte(fieldPrix.getText().trim(), errPrix, fieldPrix, v -> {
            if (v.isBlank()) return "Le prix est obligatoire.";
            try {
                double d = Double.parseDouble(v.replace(",", "."));
                if (d < 0)        return "Le prix doit être positif ou nul.";
                if (d > 99999.99) return "Le prix ne peut pas dépasser 99 999,99 TND.";
            } catch (NumberFormatException e) { return "Prix invalide — entrez un nombre (ex: 29.99)."; }
            return null;
        });
        ok &= validerTexte(fieldStock.getText().trim(), errStock, fieldStock, v -> {
            if (v.isBlank()) return "Le stock est obligatoire.";
            try {
                int i = Integer.parseInt(v);
                if (i < 0)     return "Le stock ne peut pas être négatif.";
                if (i > 99999) return "Le stock ne peut pas dépasser 99 999.";
            } catch (NumberFormatException e) { return "Stock invalide — entrez un nombre entier (ex: 50)."; }
            return null;
        });
        ok &= validerComboVal(fieldType.getValue(),      errType,      fieldType,      v -> v == null ? "Veuillez sélectionner un type." : null);
        ok &= validerComboVal(fieldCategorie.getValue(), errCategorie, fieldCategorie, v -> v == null ? "Veuillez sélectionner une catégorie." : null);

        // Image optionnelle
        String img = fieldImage.getText().trim();
        if (!img.isBlank()) {
            boolean isUrl  = img.startsWith("http://") || img.startsWith("https://") || img.startsWith("file:/");
            boolean isPath = img.contains("/") || img.contains("\\");
            if (!isUrl && !isPath) {
                afficher(errImage, fieldImage, "Format invalide — entrez une URL ou un chemin de fichier.");
                ok = false;
            } else if (!img.matches(".*\\.(png|jpg|jpeg|gif|webp|PNG|JPG|JPEG|GIF|WEBP).*")) {
                afficher(errImage, fieldImage, "Extension non reconnue — utilisez png, jpg, jpeg, gif ou webp.");
                ok = false;
            } else {
                cacher(errImage, fieldImage);
            }
        } else {
            cacher(errImage, fieldImage);
        }

        return ok;
    }

    // ── Helpers internes ──────────────────────────────────────────────────────

    @FunctionalInterface
    private interface Regle<T> { String valider(T val); }

    private static void attachTexte(TextField field, Label err, Regle<String> regle) {
        // On blur
        field.focusedProperty().addListener((obs, wasF, isF) -> {
            if (!isF) evalTexte(field.getText().trim(), err, field, regle);
        });
        // On type (efface l'erreur dès que le champ devient valide)
        field.textProperty().addListener((obs, o, n) -> {
            if (err.isVisible()) evalTexte(n.trim(), err, field, regle);
        });
    }

    private static void attachTextArea(TextArea field, Label err, Regle<String> regle) {
        field.focusedProperty().addListener((obs, wasF, isF) -> {
            if (!isF) evalTexte(field.getText().trim(), err, field, regle);
        });
        field.textProperty().addListener((obs, o, n) -> {
            if (err.isVisible()) evalTexte(n.trim(), err, field, regle);
        });
    }

    private static void attachCombo(ComboBox<String> combo, Label err, Regle<String> regle) {
        combo.valueProperty().addListener((obs, o, n) -> evalCombo(n, err, combo, regle));
        combo.focusedProperty().addListener((obs, wasF, isF) -> {
            if (!isF) evalCombo(combo.getValue(), err, combo, regle);
        });
    }

    private static void evalTexte(String val, Label err, Region field, Regle<String> regle) {
        String msg = regle.valider(val);
        if (msg != null) afficher(err, field, msg);
        else             cacher(err, field);
    }

    private static void evalCombo(String val, Label err, Region field, Regle<String> regle) {
        String msg = regle.valider(val);
        if (msg != null) afficher(err, field, msg);
        else             cacher(err, field);
    }

    private static boolean validerTexte(String val, Label err, Region field, Regle<String> regle) {
        String msg = regle.valider(val);
        if (msg != null) { afficher(err, field, msg); return false; }
        cacher(err, field); return true;
    }

    private static boolean validerComboVal(String val, Label err, Region field, Regle<String> regle) {
        String msg = regle.valider(val);
        if (msg != null) { afficher(err, field, msg); return false; }
        cacher(err, field); return true;
    }

    private static void afficher(Label err, Region field, String msg) {
        err.setText(msg);
        err.setVisible(true);
        err.setManaged(true);
        // S'assurer que la classe CSS rouge est bien présente
        if (!err.getStyleClass().contains("field-error"))
            err.getStyleClass().add("field-error");
        // Bordure rouge sur le champ
        field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-border-radius: 10px;");
    }

    private static void cacher(Label err, Region field) {
        err.setVisible(false);
        err.setManaged(false);
        err.getStyleClass().remove("field-error");
        field.setStyle("");
    }
}

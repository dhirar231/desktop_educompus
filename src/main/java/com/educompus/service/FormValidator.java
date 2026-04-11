package com.educompus.service;

import javafx.css.PseudoClass;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilitaire JavaFX pour appliquer les résultats de validation sur les champs.
 *
 * Usage :
 *   FormValidator fv = new FormValidator();
 *   fv.check(titreField, CoursValidationService.validateTitreCours(titreField.getText()));
 *   if (!fv.isValid()) { ... }
 */
public final class FormValidator {

    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

    private final Map<Control, Label> errorLabels = new HashMap<>();
    private boolean valid = true;

    /**
     * Valide un champ et applique le style d'erreur si nécessaire.
     * @param field  le contrôle JavaFX (TextField, TextArea, ComboBox...)
     * @param result le résultat de validation
     * @return this (chaînable)
     */
    public FormValidator check(Control field, ValidationResult result) {
        if (field == null) return this;
        if (result.isValid()) {
            clearError(field);
        } else {
            markError(field, result.firstError());
            valid = false;
        }
        return this;
    }

    /** Retourne true si tous les champs vérifiés sont valides. */
    public boolean isValid() {
        return valid;
    }

    /** Réinitialise l'état (à appeler avant une nouvelle validation). */
    public void reset() {
        valid = true;
    }

    /** Efface le style d'erreur sur un champ. */
    public static void clearError(Control field) {
        if (field == null) return;
        field.pseudoClassStateChanged(INVALID, false);
        field.getStyleClass().remove("field-invalid");
        Tooltip tip = field.getTooltip();
        if (tip != null && tip.getText() != null && tip.getText().startsWith("⚠")) {
            field.setTooltip(null);
        }
    }

    /** Applique le style d'erreur et un tooltip sur un champ. */
    public static void markError(Control field, String message) {
        if (field == null) return;
        field.pseudoClassStateChanged(INVALID, true);
        if (!field.getStyleClass().contains("field-invalid")) {
            field.getStyleClass().add("field-invalid");
        }
        Tooltip tip = new Tooltip("⚠ " + message);
        tip.setStyle("-fx-background-color: #d6293e; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 8px;");
        field.setTooltip(tip);
    }

    /**
     * Attache une validation en temps réel sur un TextInputControl.
     * Le champ est validé à chaque frappe.
     */
    public static void liveValidate(TextInputControl field, java.util.function.Supplier<ValidationResult> validator) {
        if (field == null || validator == null) return;
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            ValidationResult r = validator.get();
            if (r.isValid()) {
                clearError(field);
            } else {
                markError(field, r.firstError());
            }
        });
    }

    /**
     * Affiche une alerte d'erreur JavaFX avec tous les messages de validation.
     */
    public static void showErrorAlert(String title, ValidationResult result) {
        if (result == null || result.isValid()) return;
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Erreurs de saisie");
        alert.setContentText(result.allErrors());
        com.educompus.util.Dialogs.style(alert);
        alert.showAndWait();
    }
}

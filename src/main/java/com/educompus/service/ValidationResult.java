package com.educompus.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Résultat d'une validation : contient la liste des erreurs trouvées.
 */
public final class ValidationResult {

    private final List<String> errors = new ArrayList<>();

    public void addError(String message) {
        if (message != null && !message.isBlank()) {
            errors.add(message);
        }
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /** Retourne le premier message d'erreur, ou chaîne vide si valide. */
    public String firstError() {
        return errors.isEmpty() ? "" : errors.get(0);
    }

    /** Retourne tous les messages joints par un saut de ligne. */
    public String allErrors() {
        return String.join("\n", errors);
    }
}

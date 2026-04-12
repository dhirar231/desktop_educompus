package com.educompus.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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


    public String firstError() {
        return errors.isEmpty() ? "" : errors.get(0);
    }


    public String allErrors() {
        return String.join("\n", errors);
    }
}

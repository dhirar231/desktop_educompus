package com.educompus.model;

public enum KanbanStatus {
    TODO("TODO", "À faire"),
    IN_PROGRESS("IN_PROGRESS", "En cours"),
    DONE("DONE", "Terminé");

    private final String dbValue;
    private final String label;

    KanbanStatus(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String dbValue() {
        return dbValue;
    }

    public String label() {
        return label;
    }

    public static KanbanStatus fromDb(String raw) {
        if (raw == null) {
            return TODO;
        }
        for (KanbanStatus s : values()) {
            if (s.dbValue.equalsIgnoreCase(raw.trim())) {
                return s;
            }
        }
        return TODO;
    }
}


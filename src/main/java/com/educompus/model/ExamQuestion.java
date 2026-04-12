package com.educompus.model;

import java.util.ArrayList;
import java.util.List;

public final class ExamQuestion {
    private int id;
    private String text;
    private int durationSeconds;
    private final List<ExamAnswer> answers = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text == null ? "" : text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = Math.max(0, durationSeconds);
    }

    public List<ExamAnswer> getAnswers() {
        return answers;
    }
}

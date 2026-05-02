package com.educompus.model;

import java.util.List;

public final class PlacementQuestion {
    private final String language; // "en" or "fr"
    private final String text;
    private final List<String> choices;
    private final int correctIndex;

    public PlacementQuestion(String language, String text, List<String> choices, int correctIndex) {
        this.language = language;
        this.text = text;
        this.choices = choices;
        this.correctIndex = correctIndex;
    }

    public String getLanguage() { return language; }
    public String getText() { return text; }
    public List<String> getChoices() { return choices; }
    public int getCorrectIndex() { return correctIndex; }
}

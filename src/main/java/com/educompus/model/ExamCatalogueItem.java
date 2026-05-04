package com.educompus.model;

public final class ExamCatalogueItem {
    private int examId;
    private int courseId;
    private String courseTitle;
    private String courseDescription;
    private String examTitle;
    private String examDescription;
    private String levelLabel;
    private String domainLabel;
    private int questionCount;
    private int estimatedMinutes;
    // total estimated duration in seconds (preferred for precise display)
    private int estimatedSeconds;
    private boolean published;

    public int getExamId() {
        return examId;
    }

    public void setExamId(int examId) {
        this.examId = examId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle == null ? "" : courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getCourseDescription() {
        return courseDescription == null ? "" : courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }

    public String getExamTitle() {
        return examTitle == null ? "" : examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }

    public String getExamDescription() {
        return examDescription == null ? "" : examDescription;
    }

    public void setExamDescription(String examDescription) {
        this.examDescription = examDescription;
    }

    public String getLevelLabel() {
        return levelLabel == null || levelLabel.isBlank() ? "Standard" : levelLabel;
    }

    public void setLevelLabel(String levelLabel) {
        this.levelLabel = levelLabel;
    }

    public String getDomainLabel() {
        return domainLabel == null || domainLabel.isBlank() ? "Quiz" : domainLabel;
    }

    public void setDomainLabel(String domainLabel) {
        this.domainLabel = domainLabel;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = Math.max(0, questionCount);
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = Math.max(0, estimatedMinutes);
    }

    public int getEstimatedSeconds() {
        return estimatedSeconds;
    }

    public void setEstimatedSeconds(int estimatedSeconds) {
        this.estimatedSeconds = Math.max(0, estimatedSeconds);
        // keep minutes in sync for compatibility
        this.estimatedMinutes = (int) Math.max(0, Math.ceil(this.estimatedSeconds / 60.0));
    }

    public String getEstimatedDurationLabel() {
        if (estimatedSeconds <= 0) return "0:00";
        int mm = estimatedSeconds / 60;
        int ss = estimatedSeconds % 60;
        return String.format("%d:%02d", mm, ss);
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public String getStatusLabel() {
        return questionCount > 0 ? "Pret" : "Brouillon";
    }
}

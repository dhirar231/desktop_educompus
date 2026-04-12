package com.educompus.model;

public class ExamAttemptRecord {
    private String email;
    private int attempts;
    private boolean passed;
    private String certificatePath;
    private String courseName;
    private String examTitle;

    public ExamAttemptRecord() {}

    public ExamAttemptRecord(String email, int attempts, boolean passed, String certificatePath) {
        this.email = email;
        this.attempts = attempts;
        this.passed = passed;
        this.certificatePath = certificatePath;
    }

    public ExamAttemptRecord(String email, int attempts, boolean passed, String certificatePath, String courseName, String examTitle) {
        this.email = email;
        this.attempts = attempts;
        this.passed = passed;
        this.certificatePath = certificatePath;
        this.courseName = courseName;
        this.examTitle = examTitle;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public String getCertificatePath() { return certificatePath; }
    public void setCertificatePath(String certificatePath) { this.certificatePath = certificatePath; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getExamTitle() { return examTitle; }
    public void setExamTitle(String examTitle) { this.examTitle = examTitle; }
}

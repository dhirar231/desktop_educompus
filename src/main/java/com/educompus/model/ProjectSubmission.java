package com.educompus.model;

public class ProjectSubmission {
    private int id;
    private int projectId;
    private int studentId;
    private String textResponse;
    private String cahierPath;
    private String dossierPath;
    private String submittedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getTextResponse() {
        return textResponse;
    }

    public void setTextResponse(String textResponse) {
        this.textResponse = textResponse;
    }

    public String getCahierPath() {
        return cahierPath;
    }

    public void setCahierPath(String cahierPath) {
        this.cahierPath = cahierPath;
    }

    public String getDossierPath() {
        return dossierPath;
    }

    public void setDossierPath(String dossierPath) {
        this.dossierPath = dossierPath;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }
}


package com.educompus.model;

public final class VideoExplicative {
    private int id;
    private String titre;
    private String urlVideo;
    private String description;
    private String dateCreation;
    private String niveau;
    private int coursId;
    private String coursTitre;
    private int chapitreId;
    private String chapitreTitre;
    private String domaine;
    
    // Nouveaux champs pour les vidéos AI
    private boolean isAIGenerated;
    private String aiScript;
    private String generationStatus; // "PENDING", "PROCESSING", "COMPLETED", "ERROR"
    private String didVideoId; // ID de la vidéo D-ID pour le suivi

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getUrlVideo() {
        return urlVideo;
    }

    public void setUrlVideo(String urlVideo) {
        this.urlVideo = urlVideo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public int getCoursId() {
        return coursId;
    }

    public void setCoursId(int coursId) {
        this.coursId = coursId;
    }

    public String getCoursTitre() {
        return coursTitre;
    }

    public void setCoursTitre(String coursTitre) {
        this.coursTitre = coursTitre;
    }

    public int getChapitreId() {
        return chapitreId;
    }

    public void setChapitreId(int chapitreId) {
        this.chapitreId = chapitreId;
    }

    public String getChapitreTitre() {
        return chapitreTitre;
    }

    public void setChapitreTitre(String chapitreTitre) {
        this.chapitreTitre = chapitreTitre;
    }

    public String getDomaine() {
        return domaine;
    }

    public void setDomaine(String domaine) {
        this.domaine = domaine;
    }

    // Getters/Setters pour les champs AI
    public boolean isAIGenerated() {
        return isAIGenerated;
    }

    public void setAIGenerated(boolean AIGenerated) {
        isAIGenerated = AIGenerated;
    }

    public String getAiScript() {
        return aiScript;
    }

    public void setAiScript(String aiScript) {
        this.aiScript = aiScript;
    }

    public String getGenerationStatus() {
        return generationStatus;
    }

    public void setGenerationStatus(String generationStatus) {
        this.generationStatus = generationStatus;
    }

    public String getDidVideoId() {
        return didVideoId;
    }

    public void setDidVideoId(String didVideoId) {
        this.didVideoId = didVideoId;
    }
}

package com.educompus.model;

public final class Chapitre {
    private int id;
    private String titre;
    private int ordre;
    private String description;
    private String fichierC;
    private String dateCreation;
    private int coursId;
    private String coursTitre;
    private String niveau;
    private String domaine;
    private int tdCount;
    private int videoCount;

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

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFichierC() {
        return fichierC;
    }

    public void setFichierC(String fichierC) {
        this.fichierC = fichierC;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
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

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getDomaine() {
        return domaine;
    }

    public void setDomaine(String domaine) {
        this.domaine = domaine;
    }

    public int getTdCount() {
        return tdCount;
    }

    public void setTdCount(int tdCount) {
        this.tdCount = tdCount;
    }

    public int getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(int videoCount) {
        this.videoCount = videoCount;
    }

    @Override
    public String toString() {
        return titre == null || titre.isBlank() ? ("Chapitre #" + id) : titre;
    }
}

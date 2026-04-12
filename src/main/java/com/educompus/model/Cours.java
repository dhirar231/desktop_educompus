package com.educompus.model;

public final class Cours {
    private int id;
    private String titre;
    private String description;
    private String niveau;
    private String domaine;
    private String image;
    private String dateCreation;
    private String nomFormateur;
    private int dureeTotaleHeures;
    private int chapitreCount;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getNomFormateur() {
        return nomFormateur;
    }

    public void setNomFormateur(String nomFormateur) {
        this.nomFormateur = nomFormateur;
    }

    public int getDureeTotaleHeures() {
        return dureeTotaleHeures;
    }

    public void setDureeTotaleHeures(int dureeTotaleHeures) {
        this.dureeTotaleHeures = dureeTotaleHeures;
    }

    public int getChapitreCount() {
        return chapitreCount;
    }

    public void setChapitreCount(int chapitreCount) {
        this.chapitreCount = chapitreCount;
    }

    @Override
    public String toString() {
        return titre == null || titre.isBlank() ? ("Cours #" + id) : titre;
    }
}

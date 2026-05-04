package com.educompus.model;

/**
 * Paramètres de génération pour les vidéos IA.
 */
public final class ParametresGeneration {
    
    private int dureeMinutes = 3;
    private String langue = "fr";
    private String qualite = "HD";
    private String voixType = "neutre";
    private String styleNarration = "pédagogique";
    private String formatSortie = "mp4";
    private boolean soustitres = true;
    private String themeVisuel = "moderne";
    
    public ParametresGeneration() {
        // Constructeur par défaut avec valeurs par défaut
    }
    
    public int getDureeMinutes() {
        return dureeMinutes;
    }
    
    public void setDureeMinutes(int dureeMinutes) {
        this.dureeMinutes = Math.max(1, Math.min(30, dureeMinutes)); // Entre 1 et 30 minutes
    }
    
    public String getLangue() {
        return langue;
    }
    
    public void setLangue(String langue) {
        this.langue = langue == null ? "fr" : langue;
    }
    
    public String getQualite() {
        return qualite;
    }
    
    public void setQualite(String qualite) {
        this.qualite = qualite == null ? "HD" : qualite;
    }
    
    public String getVoixType() {
        return voixType;
    }
    
    public void setVoixType(String voixType) {
        this.voixType = voixType == null ? "neutre" : voixType;
    }
    
    public String getStyleNarration() {
        return styleNarration;
    }
    
    public void setStyleNarration(String styleNarration) {
        this.styleNarration = styleNarration == null ? "pédagogique" : styleNarration;
    }
    
    public String getFormatSortie() {
        return formatSortie;
    }
    
    public void setFormatSortie(String formatSortie) {
        this.formatSortie = formatSortie == null ? "mp4" : formatSortie;
    }
    
    public boolean isSoustitres() {
        return soustitres;
    }
    
    public void setSoustitres(boolean soustitres) {
        this.soustitres = soustitres;
    }
    
    public String getThemeVisuel() {
        return themeVisuel;
    }
    
    public void setThemeVisuel(String themeVisuel) {
        this.themeVisuel = themeVisuel == null ? "moderne" : themeVisuel;
    }
    
    @Override
    public String toString() {
        return String.format(
            "ParametresGeneration{durée=%d min, langue='%s', qualité='%s', voix='%s', style='%s', format='%s', sous-titres=%s, thème='%s'}",
            dureeMinutes, langue, qualite, voixType, styleNarration, formatSortie, soustitres, themeVisuel
        );
    }
    
    /**
     * Crée des paramètres par défaut pour une génération rapide.
     */
    public static ParametresGeneration parDefaut() {
        return new ParametresGeneration();
    }
    
    /**
     * Crée des paramètres optimisés pour des vidéos courtes.
     */
    public static ParametresGeneration videoCourte() {
        ParametresGeneration params = new ParametresGeneration();
        params.setDureeMinutes(2);
        params.setQualite("SD");
        params.setStyleNarration("concis");
        return params;
    }
    
    /**
     * Crée des paramètres optimisés pour des vidéos détaillées.
     */
    public static ParametresGeneration videoDetaillee() {
        ParametresGeneration params = new ParametresGeneration();
        params.setDureeMinutes(10);
        params.setQualite("Full HD");
        params.setStyleNarration("détaillé");
        params.setSoustitres(true);
        return params;
    }
}
package com.educompus.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modèle représentant le niveau de risque de désengagement d'un étudiant.
 */
public class StudentEngagementRisk {
    
    private int studentId;
    private String studentName;
    private String studentEmail;
    private int courseId;
    private String courseName;
    
    // Métriques d'engagement
    private int liveSessionAbsences;
    private int daysSinceLastConnection;
    private int unopenedChapters;
    private int totalChapters;
    private int undownloadedPdfs;
    private int totalPdfs;
    private LocalDateTime lastConnectionDate;
    
    // Score et niveau de risque
    private int riskScore;
    private RiskLevel riskLevel;
    private List<String> riskReasons;
    
    // Métadonnées
    private LocalDateTime calculatedAt;
    
    public enum RiskLevel {
        ACTIF("Actif", "#27ae60", 0, 30),
        A_SURVEILLER("À surveiller", "#f39c12", 31, 60),
        DESENGAGÉ("Désengagé", "#e74c3c", 61, Integer.MAX_VALUE);
        
        public final String label;
        public final String color;
        public final int minScore;
        public final int maxScore;
        
        RiskLevel(String label, String color, int minScore, int maxScore) {
            this.label = label;
            this.color = color;
            this.minScore = minScore;
            this.maxScore = maxScore;
        }
        
        public static RiskLevel fromScore(int score) {
            if (score <= 30) return ACTIF;
            if (score <= 60) return A_SURVEILLER;
            return DESENGAGÉ;
        }
    }
    
    public StudentEngagementRisk() {
        this.riskReasons = new ArrayList<>();
        this.calculatedAt = LocalDateTime.now();
    }
    
    // Getters et Setters
    
    public int getStudentId() {
        return studentId;
    }
    
    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }
    
    public String getStudentName() {
        return studentName;
    }
    
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    
    public String getStudentEmail() {
        return studentEmail;
    }
    
    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }
    
    public int getCourseId() {
        return courseId;
    }
    
    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }
    
    public String getCourseName() {
        return courseName;
    }
    
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    
    public int getLiveSessionAbsences() {
        return liveSessionAbsences;
    }
    
    public void setLiveSessionAbsences(int liveSessionAbsences) {
        this.liveSessionAbsences = liveSessionAbsences;
    }
    
    public int getDaysSinceLastConnection() {
        return daysSinceLastConnection;
    }
    
    public void setDaysSinceLastConnection(int daysSinceLastConnection) {
        this.daysSinceLastConnection = daysSinceLastConnection;
    }
    
    public int getUnopenedChapters() {
        return unopenedChapters;
    }
    
    public void setUnopenedChapters(int unopenedChapters) {
        this.unopenedChapters = unopenedChapters;
    }
    
    public int getTotalChapters() {
        return totalChapters;
    }
    
    public void setTotalChapters(int totalChapters) {
        this.totalChapters = totalChapters;
    }
    
    public int getUndownloadedPdfs() {
        return undownloadedPdfs;
    }
    
    public void setUndownloadedPdfs(int undownloadedPdfs) {
        this.undownloadedPdfs = undownloadedPdfs;
    }
    
    public int getTotalPdfs() {
        return totalPdfs;
    }
    
    public void setTotalPdfs(int totalPdfs) {
        this.totalPdfs = totalPdfs;
    }
    
    public LocalDateTime getLastConnectionDate() {
        return lastConnectionDate;
    }
    
    public void setLastConnectionDate(LocalDateTime lastConnectionDate) {
        this.lastConnectionDate = lastConnectionDate;
    }
    
    public int getRiskScore() {
        return riskScore;
    }
    
    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
        this.riskLevel = RiskLevel.fromScore(riskScore);
    }
    
    public RiskLevel getRiskLevel() {
        return riskLevel;
    }
    
    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }
    
    public List<String> getRiskReasons() {
        return riskReasons;
    }
    
    public void setRiskReasons(List<String> riskReasons) {
        this.riskReasons = riskReasons;
    }
    
    public void addRiskReason(String reason) {
        this.riskReasons.add(reason);
    }
    
    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }
    
    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
    
    /**
     * Retourne un résumé des raisons de risque.
     */
    public String getRiskReasonsSummary() {
        if (riskReasons.isEmpty()) {
            return "Aucun problème détecté";
        }
        return String.join(" • ", riskReasons);
    }
    
    /**
     * Retourne le pourcentage de chapitres consultés.
     */
    public double getChapterCompletionRate() {
        if (totalChapters == 0) return 100.0;
        return ((totalChapters - unopenedChapters) * 100.0) / totalChapters;
    }
    
    /**
     * Retourne le pourcentage de PDFs téléchargés.
     */
    public double getPdfDownloadRate() {
        if (totalPdfs == 0) return 100.0;
        return ((totalPdfs - undownloadedPdfs) * 100.0) / totalPdfs;
    }
}

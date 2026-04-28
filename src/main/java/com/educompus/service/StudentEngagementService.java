package com.educompus.service;

import com.educompus.model.StudentEngagementRisk;
import com.educompus.model.StudentEngagementRisk.RiskLevel;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour détecter les étudiants désengagés et calculer leur score de risque.
 */
public final class StudentEngagementService {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/educompus";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    
    // Poids des critères de risque
    private static final int WEIGHT_ABSENCE_PER_SESSION = 10;  // 10 points par absence
    private static final int WEIGHT_NO_CONNECTION_7_DAYS = 25;
    private static final int WEIGHT_NO_CONNECTION_14_DAYS = 40;
    private static final int WEIGHT_UNOPENED_CHAPTER = 5;      // 5 points par chapitre
    private static final int WEIGHT_NO_PDF_DOWNLOAD = 15;
    
    /**
     * Calcule le score de risque pour tous les étudiants d'un cours.
     */
    public List<StudentEngagementRisk> analyzeStudentEngagement(int courseId) {
        List<StudentEngagementRisk> risks = new ArrayList<>();
        
        // Trouver TOUS les étudiants (pas seulement ceux avec activité)
        String sql = """
            SELECT DISTINCT u.id, u.name, u.last_name, u.email
            FROM user u
            WHERE u.roles LIKE '%STUDENT%'
            ORDER BY u.name, u.last_name
            """;
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                int studentId = rs.getInt("id");
                String name = rs.getString("name");
                String lastName = rs.getString("last_name");
                String email = rs.getString("email");
                
                StudentEngagementRisk risk = calculateRiskForStudent(studentId, courseId, conn);
                risk.setStudentId(studentId);
                risk.setStudentName(name + " " + lastName);
                risk.setStudentEmail(email);
                
                risks.add(risk);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return risks;
    }
    
    /**
     * Calcule le score de risque pour un étudiant spécifique.
     */
    private StudentEngagementRisk calculateRiskForStudent(int studentId, int courseId, Connection conn) {
        StudentEngagementRisk risk = new StudentEngagementRisk();
        risk.setCourseId(courseId);
        
        int totalScore = 0;
        
        // 1. Analyser les absences aux sessions live
        int absences = countLiveSessionAbsences(studentId, courseId, conn);
        risk.setLiveSessionAbsences(absences);
        if (absences > 0) {
            int absenceScore = absences * WEIGHT_ABSENCE_PER_SESSION;
            totalScore += absenceScore;
            risk.addRiskReason(absences + " absence" + (absences > 1 ? "s" : "") + " aux sessions live");
        }
        
        // 2. Analyser la dernière connexion
        LocalDateTime lastConnection = getLastConnectionDate(studentId, conn);
        risk.setLastConnectionDate(lastConnection);
        
        if (lastConnection != null) {
            long daysSince = ChronoUnit.DAYS.between(lastConnection, LocalDateTime.now());
            risk.setDaysSinceLastConnection((int) daysSince);
            
            if (daysSince >= 14) {
                totalScore += WEIGHT_NO_CONNECTION_14_DAYS;
                risk.addRiskReason("Aucune connexion depuis " + daysSince + " jours");
            } else if (daysSince >= 7) {
                totalScore += WEIGHT_NO_CONNECTION_7_DAYS;
                risk.addRiskReason("Aucune connexion depuis " + daysSince + " jours");
            }
        } else {
            totalScore += WEIGHT_NO_CONNECTION_14_DAYS;
            risk.setDaysSinceLastConnection(999);
            risk.addRiskReason("Aucune connexion enregistrée");
        }
        
        // 3. Analyser les chapitres non consultés
        ChapterStats chapterStats = getChapterStats(studentId, courseId, conn);
        risk.setUnopenedChapters(chapterStats.unopened);
        risk.setTotalChapters(chapterStats.total);
        
        if (chapterStats.unopened > 0) {
            int chapterScore = chapterStats.unopened * WEIGHT_UNOPENED_CHAPTER;
            totalScore += chapterScore;
            risk.addRiskReason(chapterStats.unopened + " chapitre" + (chapterStats.unopened > 1 ? "s" : "") + " non consulté" + (chapterStats.unopened > 1 ? "s" : ""));
        }
        
        // 4. Analyser les téléchargements de PDF
        boolean hasDownloads = hasAnyPdfDownloads(studentId, courseId, conn);
        if (!hasDownloads && chapterStats.total > 0) {
            totalScore += WEIGHT_NO_PDF_DOWNLOAD;
            risk.addRiskReason("Aucun téléchargement de supports PDF");
        }
        
        risk.setRiskScore(totalScore);
        
        return risk;
    }
    
    /**
     * Compte le nombre d'absences aux sessions live.
     */
    private int countLiveSessionAbsences(int studentId, int courseId, Connection conn) {
        String sql = """
            SELECT COUNT(*) as absences
            FROM session_live sl
            WHERE sl.cours_id = ?
              AND sl.statut = 'TERMINEE'
              AND sl.id NOT IN (
                  SELECT session_id 
                  FROM session_attendance 
                  WHERE student_id = ?
              )
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, courseId);
            stmt.setInt(2, studentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("absences");
                }
            }
        } catch (SQLException e) {
            // Table session_attendance peut ne pas exister
            return 0;
        }
        
        return 0;
    }
    
    /**
     * Récupère la date de dernière connexion d'un étudiant.
     */
    private LocalDateTime getLastConnectionDate(int studentId, Connection conn) {
        String sql = """
            SELECT MAX(date_connexion) as last_connection
            FROM user_activity_log
            WHERE student_id = ?
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("last_connection");
                    if (ts != null) {
                        return ts.toLocalDateTime();
                    }
                }
            }
        } catch (SQLException e) {
            // Table user_activity_log peut ne pas exister
        }
        
        return null;
    }
    
    /**
     * Récupère les statistiques des chapitres.
     */
    private ChapterStats getChapterStats(int studentId, int courseId, Connection conn) {
        ChapterStats stats = new ChapterStats();
        
        // Compter le total de chapitres
        String sqlTotal = "SELECT COUNT(*) as total FROM chapitre WHERE cours_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sqlTotal)) {
            stmt.setInt(1, courseId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.total = rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Compter les chapitres consultés
        String sqlOpened = """
            SELECT COUNT(DISTINCT chapitre_id) as opened
            FROM chapitre_progress
            WHERE student_id = ? AND completed = TRUE
            AND chapitre_id IN (SELECT id FROM chapitre WHERE cours_id = ?)
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sqlOpened)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int opened = rs.getInt("opened");
                    stats.unopened = stats.total - opened;
                }
            }
        } catch (SQLException e) {
            // Si la table n'existe pas, tous les chapitres sont non consultés
            stats.unopened = stats.total;
        }
        
        return stats;
    }
    
    /**
     * Vérifie si l'étudiant a téléchargé des PDFs.
     */
    private boolean hasAnyPdfDownloads(int studentId, int courseId, Connection conn) {
        String sql = """
            SELECT COUNT(*) as downloads
            FROM pdf_download_log
            WHERE student_id = ? AND course_id = ?
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("downloads") > 0;
                }
            }
        } catch (SQLException e) {
            // Table pdf_download_log peut ne pas exister
        }
        
        return false;
    }
    
    /**
     * Récupère les statistiques globales d'engagement.
     */
    public EngagementStats getEngagementStats(int courseId) {
        List<StudentEngagementRisk> risks = analyzeStudentEngagement(courseId);
        
        EngagementStats stats = new EngagementStats();
        stats.totalStudents = risks.size();
        
        for (StudentEngagementRisk risk : risks) {
            switch (risk.getRiskLevel()) {
                case ACTIF -> stats.activeStudents++;
                case A_SURVEILLER -> stats.atRiskStudents++;
                case DESENGAGÉ -> stats.disengagedStudents++;
            }
        }
        
        return stats;
    }
    
    // Classes internes
    
    private static class ChapterStats {
        int total = 0;
        int unopened = 0;
    }
    
    public static class EngagementStats {
        public int totalStudents = 0;
        public int activeStudents = 0;
        public int atRiskStudents = 0;
        public int disengagedStudents = 0;
        
        public double getActivePercentage() {
            return totalStudents > 0 ? (activeStudents * 100.0) / totalStudents : 0;
        }
        
        public double getAtRiskPercentage() {
            return totalStudents > 0 ? (atRiskStudents * 100.0) / totalStudents : 0;
        }
        
        public double getDisengagedPercentage() {
            return totalStudents > 0 ? (disengagedStudents * 100.0) / totalStudents : 0;
        }
    }
}

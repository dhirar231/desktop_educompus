package com.educompus.service;

import com.educompus.repository.CourseManagementRepository;
import com.educompus.repository.EducompusDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour les statistiques de gestion de contenu pédagogique.
 */
public final class ContentStatisticsService {
    
    private final CourseManagementRepository repository;
    
    public ContentStatisticsService() {
        this.repository = new CourseManagementRepository();
    }
    
    /**
     * Obtient les statistiques complètes du contenu.
     */
    public ContentStatistics getContentStatistics() {
        String sql = "SELECT * FROM v_content_statistics";
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return new ContentStatistics(
                    rs.getInt("total_cours"),
                    rs.getInt("cours_importants"),
                    rs.getInt("cours_sur_drive"),
                    rs.getInt("total_chapitres"),
                    rs.getInt("chapitres_importants"),
                    rs.getInt("chapitres_sur_drive"),
                    rs.getInt("total_tds"),
                    rs.getInt("tds_importants"),
                    rs.getInt("tds_sur_drive"),
                    rs.getInt("total_videos"),
                    rs.getInt("videos_importants"),
                    rs.getInt("videos_sur_drive"),
                    rs.getInt("total_fichiers_drive"),
                    rs.getLong("taille_totale_drive")
                );
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des statistiques: " + e.getMessage());
        }
        
        return new ContentStatistics(); // Statistiques vides en cas d'erreur
    }
    
    /**
     * Obtient la liste du contenu important non uploadé.
     */
    public List<ContentItem> getImportantContentNotUploaded() {
        String sql = "SELECT * FROM v_content_important_non_uploade ORDER BY date_creation DESC";
        List<ContentItem> items = new ArrayList<>();
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                items.add(new ContentItem(
                    rs.getString("type_contenu"),
                    rs.getInt("id"),
                    rs.getString("titre"),
                    rs.getBoolean("important"),
                    rs.getString("drive_link"),
                    rs.getString("date_creation")
                ));
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du contenu non uploadé: " + e.getMessage());
        }
        
        return items;
    }
    
    /**
     * Enregistre un upload Google Drive.
     */
    public void recordDriveUpload(String contentType, int contentId, String driveFileId, 
                                 String driveLink, String fileName, long fileSize) {
        String sql = """
            INSERT OR REPLACE INTO drive_uploads 
            (content_type, content_id, drive_file_id, drive_link, file_name, file_size, upload_date, status)
            VALUES (?, ?, ?, ?, ?, ?, datetime('now'), 'uploaded')
            """;
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, contentType);
            stmt.setInt(2, contentId);
            stmt.setString(3, driveFileId);
            stmt.setString(4, driveLink);
            stmt.setString(5, fileName);
            stmt.setLong(6, fileSize);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'enregistrement de l'upload: " + e.getMessage());
        }
    }
    
    /**
     * Marque un upload comme supprimé.
     */
    public void markUploadAsDeleted(String contentType, int contentId) {
        String sql = "UPDATE drive_uploads SET status = 'deleted' WHERE content_type = ? AND content_id = ?";
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, contentType);
            stmt.setInt(2, contentId);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Erreur lors du marquage de suppression: " + e.getMessage());
        }
    }
    
    /**
     * Obtient l'historique des uploads récents.
     */
    public List<DriveUploadRecord> getRecentUploads(int days) {
        String sql = """
            SELECT * FROM drive_uploads 
            WHERE upload_date >= date('now', '-' || ? || ' days') 
            ORDER BY upload_date DESC
            """;
        
        List<DriveUploadRecord> records = new ArrayList<>();
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, days);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(new DriveUploadRecord(
                        rs.getInt("id"),
                        rs.getString("content_type"),
                        rs.getInt("content_id"),
                        rs.getString("drive_file_id"),
                        rs.getString("drive_link"),
                        rs.getString("file_name"),
                        rs.getLong("file_size"),
                        rs.getString("upload_date"),
                        rs.getString("status")
                    ));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des uploads récents: " + e.getMessage());
        }
        
        return records;
    }
    
    /**
     * Sauvegarde un snapshot des statistiques.
     */
    public void saveStatisticsSnapshot() {
        ContentStatistics stats = getContentStatistics();
        
        String sql = """
            INSERT OR REPLACE INTO content_stats 
            (date_snapshot, total_cours, cours_importants, total_chapitres, chapitres_importants,
             total_tds, tds_importants, total_videos, videos_importants, 
             drive_storage_used, drive_files_count)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, LocalDate.now().toString());
            stmt.setInt(2, stats.getTotalCours());
            stmt.setInt(3, stats.getCoursImportants());
            stmt.setInt(4, stats.getTotalChapitres());
            stmt.setInt(5, stats.getChapitresImportants());
            stmt.setInt(6, stats.getTotalTds());
            stmt.setInt(7, stats.getTdsImportants());
            stmt.setInt(8, stats.getTotalVideos());
            stmt.setInt(9, stats.getVideosImportants());
            stmt.setLong(10, stats.getTailleTotaleDrive());
            stmt.setInt(11, stats.getTotalFichiersDrive());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Erreur lors de la sauvegarde du snapshot: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // CLASSES DE DONNÉES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Statistiques complètes du contenu.
     */
    public static class ContentStatistics {
        private final int totalCours;
        private final int coursImportants;
        private final int coursSurDrive;
        private final int totalChapitres;
        private final int chapitresImportants;
        private final int chapitresSurDrive;
        private final int totalTds;
        private final int tdsImportants;
        private final int tdsSurDrive;
        private final int totalVideos;
        private final int videosImportants;
        private final int videosSurDrive;
        private final int totalFichiersDrive;
        private final long tailleTotaleDrive;
        
        public ContentStatistics() {
            this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0L);
        }
        
        public ContentStatistics(int totalCours, int coursImportants, int coursSurDrive,
                               int totalChapitres, int chapitresImportants, int chapitresSurDrive,
                               int totalTds, int tdsImportants, int tdsSurDrive,
                               int totalVideos, int videosImportants, int videosSurDrive,
                               int totalFichiersDrive, long tailleTotaleDrive) {
            this.totalCours = totalCours;
            this.coursImportants = coursImportants;
            this.coursSurDrive = coursSurDrive;
            this.totalChapitres = totalChapitres;
            this.chapitresImportants = chapitresImportants;
            this.chapitresSurDrive = chapitresSurDrive;
            this.totalTds = totalTds;
            this.tdsImportants = tdsImportants;
            this.tdsSurDrive = tdsSurDrive;
            this.totalVideos = totalVideos;
            this.videosImportants = videosImportants;
            this.videosSurDrive = videosSurDrive;
            this.totalFichiersDrive = totalFichiersDrive;
            this.tailleTotaleDrive = tailleTotaleDrive;
        }
        
        // Getters
        public int getTotalCours() { return totalCours; }
        public int getCoursImportants() { return coursImportants; }
        public int getCoursSurDrive() { return coursSurDrive; }
        public int getTotalChapitres() { return totalChapitres; }
        public int getChapitresImportants() { return chapitresImportants; }
        public int getChapitresSurDrive() { return chapitresSurDrive; }
        public int getTotalTds() { return totalTds; }
        public int getTdsImportants() { return tdsImportants; }
        public int getTdsSurDrive() { return tdsSurDrive; }
        public int getTotalVideos() { return totalVideos; }
        public int getVideosImportants() { return videosImportants; }
        public int getVideosSurDrive() { return videosSurDrive; }
        public int getTotalFichiersDrive() { return totalFichiersDrive; }
        public long getTailleTotaleDrive() { return tailleTotaleDrive; }
        
        public int getTotalContenu() {
            return totalCours + totalChapitres + totalTds + totalVideos;
        }
        
        public int getTotalImportant() {
            return coursImportants + chapitresImportants + tdsImportants + videosImportants;
        }
        
        public double getPourcentageImportant() {
            int total = getTotalContenu();
            return total > 0 ? (getTotalImportant() * 100.0 / total) : 0.0;
        }
        
        public String formatTailleDrive() {
            if (tailleTotaleDrive < 1024) return tailleTotaleDrive + " B";
            if (tailleTotaleDrive < 1024 * 1024) return String.format("%.1f KB", tailleTotaleDrive / 1024.0);
            if (tailleTotaleDrive < 1024 * 1024 * 1024) return String.format("%.1f MB", tailleTotaleDrive / (1024.0 * 1024.0));
            return String.format("%.1f GB", tailleTotaleDrive / (1024.0 * 1024.0 * 1024.0));
        }
        
        @Override
        public String toString() {
            return String.format("""
                📊 STATISTIQUES DE CONTENU
                
                📚 Cours: %d total, %d importants (%d sur Drive)
                📑 Chapitres: %d total, %d importants (%d sur Drive)
                📝 TDs: %d total, %d importants (%d sur Drive)
                🎬 Vidéos: %d total, %d importants (%d sur Drive)
                
                ☁️ Google Drive: %d fichiers, %s utilisés
                📈 Contenu important: %.1f%% du total
                """,
                totalCours, coursImportants, coursSurDrive,
                totalChapitres, chapitresImportants, chapitresSurDrive,
                totalTds, tdsImportants, tdsSurDrive,
                totalVideos, videosImportants, videosSurDrive,
                totalFichiersDrive, formatTailleDrive(),
                getPourcentageImportant()
            );
        }
    }
    
    /**
     * Élément de contenu.
     */
    public static class ContentItem {
        private final String typeContenu;
        private final int id;
        private final String titre;
        private final boolean important;
        private final String driveLink;
        private final String dateCreation;
        
        public ContentItem(String typeContenu, int id, String titre, boolean important, 
                          String driveLink, String dateCreation) {
            this.typeContenu = typeContenu;
            this.id = id;
            this.titre = titre;
            this.important = important;
            this.driveLink = driveLink;
            this.dateCreation = dateCreation;
        }
        
        public String getTypeContenu() { return typeContenu; }
        public int getId() { return id; }
        public String getTitre() { return titre; }
        public boolean isImportant() { return important; }
        public String getDriveLink() { return driveLink; }
        public String getDateCreation() { return dateCreation; }
    }
    
    /**
     * Enregistrement d'upload Google Drive.
     */
    public static class DriveUploadRecord {
        private final int id;
        private final String contentType;
        private final int contentId;
        private final String driveFileId;
        private final String driveLink;
        private final String fileName;
        private final long fileSize;
        private final String uploadDate;
        private final String status;
        
        public DriveUploadRecord(int id, String contentType, int contentId, String driveFileId,
                               String driveLink, String fileName, long fileSize, 
                               String uploadDate, String status) {
            this.id = id;
            this.contentType = contentType;
            this.contentId = contentId;
            this.driveFileId = driveFileId;
            this.driveLink = driveLink;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.uploadDate = uploadDate;
            this.status = status;
        }
        
        public int getId() { return id; }
        public String getContentType() { return contentType; }
        public int getContentId() { return contentId; }
        public String getDriveFileId() { return driveFileId; }
        public String getDriveLink() { return driveLink; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public String getUploadDate() { return uploadDate; }
        public String getStatus() { return status; }
    }
}
package com.educompus.service;

import com.educompus.model.Chapitre;
import com.educompus.model.Cours;
import com.educompus.model.Td;
import com.educompus.model.VideoExplicative;
import com.educompus.repository.CourseManagementRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service principal de gestion de contenu pédagogique avec intégration Google Drive sélective.
 * 
 * Règles métier :
 * - Seuls les contenus marqués comme "importants" sont uploadés vers Google Drive
 * - Tous les contenus sont sauvegardés en base de données
 * - Les liens Google Drive sont stockés dans le champ drive_link
 */
public final class ContentManagementService {
    
    public final CourseManagementRepository repository;
    private final GoogleDriveService driveService;
    private final CoursService coursService;
    private final ChapitreService chapitreService;
    private final TDService tdService;
    private final VideoExplicatifService videoService;
    private final ExecutorService uploadExecutor;
    
    public ContentManagementService() throws IOException, GeneralSecurityException {
        this.repository = new CourseManagementRepository();
        this.driveService = new GoogleDriveService();
        this.coursService = new CoursService();
        this.chapitreService = new ChapitreService();
        this.tdService = new TDService();
        this.videoService = new VideoExplicatifService();
        this.uploadExecutor = Executors.newFixedThreadPool(3); // Pool pour uploads asynchrones
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // GESTION DES COURS
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Crée un nouveau cours avec upload conditionnel vers Google Drive.
     * 
     * @param cours Le cours à créer
     * @param important Si true, upload vers Google Drive
     * @param filePath Chemin du fichier associé (optionnel)
     * @return ContentOperationResult avec les détails de l'opération
     */
    public ContentOperationResult createCours(Cours cours, boolean important, String filePath) {
        try {
            // 1. Validation du cours
            ValidationResult validation = coursService.validerSansException(cours);
            if (!validation.isValid()) {
                return ContentOperationResult.error("Validation échouée: " + validation.allErrors());
            }
            
            // 2. Sauvegarde en base de données
            repository.createCours(cours);
            
            // 3. Upload conditionnel vers Google Drive
            if (important && filePath != null && !filePath.isBlank()) {
                return uploadCoursToDrive(cours, filePath);
            }
            
            return ContentOperationResult.success(
                "Cours créé avec succès (stockage local uniquement)",
                null,
                false
            );
            
        } catch (Exception e) {
            return ContentOperationResult.error("Erreur lors de la création du cours: " + e.getMessage());
        }
    }
    
    /**
     * Met à jour un cours existant avec gestion de l'upload Google Drive.
     */
    public ContentOperationResult updateCours(Cours cours, boolean important, String filePath) {
        try {
            // 1. Validation
            ValidationResult validation = coursService.validerSansException(cours);
            if (!validation.isValid()) {
                return ContentOperationResult.error("Validation échouée: " + validation.allErrors());
            }
            
            // 2. Mise à jour en base
            repository.updateCours(cours);
            
            // 3. Gestion Google Drive
            if (important && filePath != null && !filePath.isBlank()) {
                // Supprimer l'ancien fichier s'il existe
                if (cours.getDriveFolderId() != null && !cours.getDriveFolderId().isBlank()) {
                    try {
                        driveService.deleteFile(cours.getDriveFolderId());
                    } catch (IOException e) {
                        System.err.println("Erreur suppression ancien fichier Drive: " + e.getMessage());
                    }
                }
                
                // Upload du nouveau fichier
                return uploadCoursToDrive(cours, filePath);
            }
            
            return ContentOperationResult.success(
                "Cours mis à jour avec succès",
                null,
                false
            );
            
        } catch (Exception e) {
            return ContentOperationResult.error("Erreur lors de la mise à jour du cours: " + e.getMessage());
        }
    }
    
    /**
     * Upload un cours vers Google Drive de manière asynchrone.
     */
    private ContentOperationResult uploadCoursToDrive(Cours cours, String filePath) {
        try {
            GoogleDriveService.DriveUploadResult result = driveService.uploadCoursFile(
                filePath,
                generateCoursFileName(cours),
                cours.getTitre()
            );
            
            // Mettre à jour le cours avec le lien Drive
            cours.setDriveFolderId(result.getFileId());
            repository.updateCours(cours);
            
            return ContentOperationResult.success(
                "Cours créé et uploadé vers Google Drive",
                result.getShareableLink(),
                true
            );
            
        } catch (IOException e) {
            return ContentOperationResult.error("Erreur upload Google Drive: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // GESTION DES CHAPITRES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Crée un nouveau chapitre avec upload conditionnel.
     */
    public ContentOperationResult createChapitre(Chapitre chapitre, boolean important, String filePath) {
        try {
            // 1. Validation
            ValidationResult validation = chapitreService.validerSansException(chapitre);
            if (!validation.isValid()) {
                return ContentOperationResult.error("Validation échouée: " + validation.allErrors());
            }
            
            // 2. Sauvegarde en base
            repository.createChapitre(chapitre);
            
            // 3. Upload conditionnel
            if (important && filePath != null && !filePath.isBlank()) {
                return uploadChapitreTorive(chapitre, filePath);
            }
            
            return ContentOperationResult.success(
                "Chapitre créé avec succès (stockage local uniquement)",
                null,
                false
            );
            
        } catch (Exception e) {
            return ContentOperationResult.error("Erreur lors de la création du chapitre: " + e.getMessage());
        }
    }
    
    /**
     * Upload un chapitre vers Google Drive.
     */
    private ContentOperationResult uploadChapitreTorive(Chapitre chapitre, String filePath) {
        try {
            // Obtenir le titre du cours parent
            String coursTitle = getCoursTitle(chapitre.getCoursId());
            
            GoogleDriveService.DriveUploadResult result = driveService.uploadChapitreFile(
                filePath,
                generateChapitreFileName(chapitre),
                coursTitle,
                chapitre.getTitre()
            );
            
            // Mettre à jour le chapitre avec le lien Drive (utiliser fichierC pour stocker l'ID)
            chapitre.setFichierC(result.getFileId());
            repository.updateChapitre(chapitre);
            
            return ContentOperationResult.success(
                "Chapitre créé et uploadé vers Google Drive",
                result.getShareableLink(),
                true
            );
            
        } catch (IOException e) {
            return ContentOperationResult.error("Erreur upload Google Drive: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // GESTION DES TDs
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Crée un nouveau TD avec upload conditionnel.
     */
    public ContentOperationResult createTd(Td td, boolean important, String filePath) {
        try {
            // 1. Validation
            ValidationResult validation = tdService.validerSansException(td);
            if (!validation.isValid()) {
                return ContentOperationResult.error("Validation échouée: " + validation.allErrors());
            }
            
            // 2. Sauvegarde en base
            repository.createTd(td);
            
            // 3. Upload conditionnel
            if (important && filePath != null && !filePath.isBlank()) {
                return uploadTdToDrive(td, filePath);
            }
            
            return ContentOperationResult.success(
                "TD créé avec succès (stockage local uniquement)",
                null,
                false
            );
            
        } catch (Exception e) {
            return ContentOperationResult.error("Erreur lors de la création du TD: " + e.getMessage());
        }
    }
    
    /**
     * Upload un TD vers Google Drive.
     */
    private ContentOperationResult uploadTdToDrive(Td td, String filePath) {
        try {
            String coursTitle = getCoursTitle(td.getCoursId());
            
            GoogleDriveService.DriveUploadResult result = driveService.uploadTdFile(
                filePath,
                generateTdFileName(td),
                coursTitle,
                td.getTitre()
            );
            
            // Mettre à jour le TD avec le lien Drive
            td.setFichier(result.getFileId());
            repository.updateTd(td);
            
            return ContentOperationResult.success(
                "TD créé et uploadé vers Google Drive",
                result.getShareableLink(),
                true
            );
            
        } catch (IOException e) {
            return ContentOperationResult.error("Erreur upload Google Drive: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // GESTION DES VIDÉOS
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Crée une nouvelle vidéo avec upload conditionnel.
     */
    public ContentOperationResult createVideo(VideoExplicative video, boolean important, String filePath) {
        try {
            // 1. Validation
            ValidationResult validation = videoService.validerSansException(video);
            if (!validation.isValid()) {
                return ContentOperationResult.error("Validation échouée: " + validation.allErrors());
            }
            
            // 2. Sauvegarde en base
            repository.createVideo(video);
            
            // 3. Upload conditionnel
            if (important && filePath != null && !filePath.isBlank()) {
                return uploadVideoToDrive(video, filePath);
            }
            
            return ContentOperationResult.success(
                "Vidéo créée avec succès (stockage local uniquement)",
                null,
                false
            );
            
        } catch (Exception e) {
            return ContentOperationResult.error("Erreur lors de la création de la vidéo: " + e.getMessage());
        }
    }
    
    /**
     * Upload une vidéo vers Google Drive.
     */
    private ContentOperationResult uploadVideoToDrive(VideoExplicative video, String filePath) {
        try {
            String coursTitle = getCoursTitle(video.getCoursId());
            
            GoogleDriveService.DriveUploadResult result = driveService.uploadVideoFile(
                filePath,
                generateVideoFileName(video),
                coursTitle,
                video.getTitre()
            );
            
            // Mettre à jour la vidéo avec le lien Drive
            video.setUrlVideo(result.getShareableLink());
            repository.updateVideo(video);
            
            return ContentOperationResult.success(
                "Vidéo créée et uploadée vers Google Drive",
                result.getShareableLink(),
                true
            );
            
        } catch (IOException e) {
            return ContentOperationResult.error("Erreur upload Google Drive: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Obtient le titre d'un cours par son ID.
     */
    private String getCoursTitle(int coursId) {
        try {
            return repository.listCours("").stream()
                    .filter(c -> c.getId() == coursId)
                    .findFirst()
                    .map(Cours::getTitre)
                    .orElse("Cours_" + coursId);
        } catch (Exception e) {
            return "Cours_" + coursId;
        }
    }
    
    /**
     * Génère un nom de fichier pour un cours.
     */
    private String generateCoursFileName(Cours cours) {
        return sanitizeFileName("Cours_" + cours.getTitre() + "_" + cours.getId() + ".pdf");
    }
    
    /**
     * Génère un nom de fichier pour un chapitre.
     */
    private String generateChapitreFileName(Chapitre chapitre) {
        return sanitizeFileName("Chapitre_" + chapitre.getOrdre() + "_" + chapitre.getTitre() + "_" + chapitre.getId() + ".pdf");
    }
    
    /**
     * Génère un nom de fichier pour un TD.
     */
    private String generateTdFileName(Td td) {
        return sanitizeFileName("TD_" + td.getTitre() + "_" + td.getId() + ".pdf");
    }
    
    /**
     * Génère un nom de fichier pour une vidéo.
     */
    private String generateVideoFileName(VideoExplicative video) {
        return sanitizeFileName("Video_" + video.getTitre() + "_" + video.getId() + ".mp4");
    }
    
    /**
     * Nettoie un nom de fichier pour qu'il soit compatible.
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "fichier_sans_nom.pdf";
        }
        
        return fileName.replaceAll("[<>:\"/\\\\|?*]", "_")
                      .replaceAll("\\s+", "_")
                      .trim();
    }
    
    /**
     * Vérifie si Google Drive est disponible.
     */
    public boolean isDriveAvailable() {
        return driveService.isAvailable();
    }
    
    /**
     * Obtient des informations sur l'utilisation du stockage Google Drive.
     */
    public GoogleDriveService.DriveStorageInfo getStorageInfo() throws IOException {
        return driveService.getStorageInfo();
    }
    
    /**
     * Ferme le service et libère les ressources.
     */
    public void shutdown() {
        if (uploadExecutor != null && !uploadExecutor.isShutdown()) {
            uploadExecutor.shutdown();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // CLASSE DE RÉSULTAT D'OPÉRATION
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Résultat d'une opération de gestion de contenu.
     */
    public static class ContentOperationResult {
        private final boolean success;
        private final String message;
        private final String driveLink;
        private final boolean uploadedToDrive;
        
        private ContentOperationResult(boolean success, String message, String driveLink, boolean uploadedToDrive) {
            this.success = success;
            this.message = message;
            this.driveLink = driveLink;
            this.uploadedToDrive = uploadedToDrive;
        }
        
        public static ContentOperationResult success(String message, String driveLink, boolean uploadedToDrive) {
            return new ContentOperationResult(true, message, driveLink, uploadedToDrive);
        }
        
        public static ContentOperationResult error(String message) {
            return new ContentOperationResult(false, message, null, false);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getDriveLink() { return driveLink; }
        public boolean isUploadedToDrive() { return uploadedToDrive; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(success ? "✅ SUCCÈS" : "❌ ERREUR");
            sb.append(": ").append(message);
            if (uploadedToDrive && driveLink != null) {
                sb.append("\n🔗 Lien Google Drive: ").append(driveLink);
            }
            return sb.toString();
        }
    }
}
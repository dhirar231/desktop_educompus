package com.educompus.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.Permission;

/**
 * Service pour l'intégration Google Drive.
 * Gère l'upload sélectif des contenus pédagogiques importants.
 */
public final class GoogleDriveService {
    
    private static final String APPLICATION_NAME = "EduCompus Content Management";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    
    // Dossiers organisationnels sur Google Drive
    private static final String ROOT_FOLDER_NAME = "Gestion Cours";
    private static final String COURS_FOLDER_NAME = "Cours";
    private static final String CHAPITRES_FOLDER_NAME = "Chapitres";
    private static final String TDS_FOLDER_NAME = "Travaux Dirigés";
    private static final String VIDEOS_FOLDER_NAME = "Vidéos Explicatives";
    
    private Drive driveService;
    private String rootFolderId;
    
    public GoogleDriveService() throws IOException, GeneralSecurityException {
        this.driveService = buildDriveService();
        this.rootFolderId = getOrCreateRootFolder();
    }
    
    /**
     * Construit le service Google Drive avec authentification.
     */
    private Drive buildDriveService() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
    
    /**
     * Obtient les credentials pour l'authentification Google.
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Charger les secrets client depuis le fichier credentials.json
        java.io.InputStream in = GoogleDriveService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new java.io.FileNotFoundException("Fichier de credentials non trouvé: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new java.io.InputStreamReader(in));
        
        // Construire le flow d'autorisation et déclencher la demande d'autorisation utilisateur
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(0).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    
    /**
     * Obtient ou crée le dossier racine EduCompus sur Google Drive.
     */
    private String getOrCreateRootFolder() throws IOException {
        try {
            String query = "name='" + ROOT_FOLDER_NAME + "' and mimeType='application/vnd.google-apps.folder' and trashed=false";
            com.google.api.services.drive.model.FileList result = driveService.files().list()
                    .setQ(query).setSpaces("drive").execute();
            List<com.google.api.services.drive.model.File> files = result.getFiles();
            if (files != null && !files.isEmpty()) return files.get(0).getId();
        } catch (Exception ignored) {
            // DRIVE_FILE scope: listing may return empty – just create a new folder
        }

        // Créer le dossier racine "Gestion Cours"
        com.google.api.services.drive.model.File folderMetadata = new com.google.api.services.drive.model.File();
        folderMetadata.setName(ROOT_FOLDER_NAME);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        com.google.api.services.drive.model.File folder = driveService.files().create(folderMetadata)
                .setFields("id").execute();
        try { makeFilePublic(folder.getId()); } catch (Exception ignored) {}
        return folder.getId();
    }
    
    /**
     * Upload un fichier de cours vers Google Drive.
     * 
     * @param filePath Chemin local du fichier
     * @param fileName Nom du fichier sur Drive
     * @param coursTitle Titre du cours pour l'organisation
     * @return DriveUploadResult contenant l'ID du fichier et le lien de partage
     */
    public DriveUploadResult uploadCoursFile(String filePath, String fileName, String coursTitle) throws IOException {
        String coursFolderId = getOrCreateCoursFolder(coursTitle);
        return uploadFile(filePath, fileName, coursFolderId, "Cours: " + coursTitle);
    }
    
    /**
     * Upload un fichier de chapitre vers Google Drive dans la structure hiérarchique.
     * Structure: Gestion Cours > [Cours] > [Chapitre] > fichier_chapitre.pdf
     */
    public DriveUploadResult uploadChapitreFile(String filePath, String fileName, String coursTitle, String chapitreTitle) throws IOException {
        String coursFolderId = getOrCreateCoursFolder(coursTitle);
        String chapitreFolderId = getOrCreateSubFolder(coursFolderId, sanitizeFolderName(chapitreTitle));
        return uploadFile(filePath, fileName, chapitreFolderId, "Chapitre: " + chapitreTitle);
    }
    
    /**
     * Upload un fichier de TD vers Google Drive dans la structure hiérarchique.
     * Structure: Gestion Cours > [Cours] > [Chapitre] > TDs > fichier_td.pdf
     */
    public DriveUploadResult uploadTdFile(String filePath, String fileName, String coursTitle, String chapitreTitle) throws IOException {
        String coursFolderId = getOrCreateCoursFolder(coursTitle);
        String chapitreFolderId = getOrCreateSubFolder(coursFolderId, sanitizeFolderName(chapitreTitle));
        String tdsFolderId = getOrCreateSubFolder(chapitreFolderId, TDS_FOLDER_NAME);
        return uploadFile(filePath, fileName, tdsFolderId, "TD du chapitre: " + chapitreTitle);
    }
    
    /**
     * Upload un fichier vidéo vers Google Drive dans la structure hiérarchique.
     * Structure: Gestion Cours > [Cours] > [Chapitre] > Vidéos > fichier_video.mp4
     */
    public DriveUploadResult uploadVideoFile(String filePath, String fileName, String coursTitle, String chapitreTitle) throws IOException {
        String coursFolderId = getOrCreateCoursFolder(coursTitle);
        String chapitreFolderId = getOrCreateSubFolder(coursFolderId, sanitizeFolderName(chapitreTitle));
        String videosFolderId = getOrCreateSubFolder(chapitreFolderId, VIDEOS_FOLDER_NAME);
        return uploadFile(filePath, fileName, videosFolderId, "Vidéo du chapitre: " + chapitreTitle);
    }
    
    /**
     * Obtient ou crée un dossier pour un cours spécifique.
     * Structure: Gestion Cours > Cours > [Nom du Cours]
     */
    public String getOrCreateCoursFolder(String coursTitle) throws IOException {
        // Créer d'abord le dossier "Cours" dans "Gestion Cours"
        String coursFolderId = getOrCreateSubFolder(rootFolderId, COURS_FOLDER_NAME);
        
        // Puis créer le dossier spécifique du cours
        String safeFolderName = sanitizeFolderName(coursTitle);
        return getOrCreateSubFolder(coursFolderId, safeFolderName);
    }
    
    /**
     * Obtient ou crée un sous-dossier dans un dossier parent.
     */
    public String getOrCreateSubFolder(String parentFolderId, String folderName) throws IOException {
        try {
            String query = "name='" + folderName + "' and mimeType='application/vnd.google-apps.folder' and '" + parentFolderId + "' in parents and trashed=false";
            com.google.api.services.drive.model.FileList result = driveService.files().list()
                    .setQ(query).setSpaces("drive").execute();
            List<com.google.api.services.drive.model.File> files = result.getFiles();
            if (files != null && !files.isEmpty()) return files.get(0).getId();
        } catch (Exception ignored) {}

        com.google.api.services.drive.model.File folderMetadata = new com.google.api.services.drive.model.File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        folderMetadata.setParents(Collections.singletonList(parentFolderId));
        com.google.api.services.drive.model.File folder = driveService.files().create(folderMetadata)
                .setFields("id").execute();
        try { makeFilePublic(folder.getId()); } catch (Exception ignored) {}
        return folder.getId();
    }
    
    /**
     * Upload un fichier vers un dossier spécifique sur Google Drive.
     */
    private DriveUploadResult uploadFile(String filePath, String fileName, String parentFolderId, String description) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Fichier non trouvé: " + filePath);
        }
        
        // Métadonnées du fichier
        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName(fileName);
        fileMetadata.setDescription(description);
        fileMetadata.setParents(Collections.singletonList(parentFolderId));
        
        // Contenu du fichier
        String mimeType = getMimeType(fileName);
        FileContent mediaContent = new FileContent(mimeType, file);
        
        // Upload du fichier
        com.google.api.services.drive.model.File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink, webContentLink")
                .execute();
        
        // Rendre le fichier public en lecture
        makeFilePublic(uploadedFile.getId());
        
        // Générer le lien de partage public
        String shareableLink = generateShareableLink(uploadedFile.getId());
        
        return new DriveUploadResult(
            uploadedFile.getId(),
            shareableLink,
            uploadedFile.getWebViewLink(),
            uploadedFile.getWebContentLink(),
            file.length()
        );
    }
    
    /**
     * Rend un fichier ou dossier public en lecture.
     */
    private void makeFilePublic(String fileId) throws IOException {
        Permission permission = new Permission();
        permission.setType("anyone");
        permission.setRole("reader");
        
        driveService.permissions().create(fileId, permission).execute();
    }
    
    /**
     * Génère un lien de partage public pour un fichier.
     */
    private String generateShareableLink(String fileId) {
        return "https://drive.google.com/file/d/" + fileId + "/view?usp=sharing";
    }
    
    /**
     * Détermine le type MIME d'un fichier basé sur son extension.
     */
    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            case "mov" -> "video/quicktime";
            case "wmv" -> "video/x-ms-wmv";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
    
    /**
     * Nettoie un nom de dossier pour qu'il soit compatible avec Google Drive.
     */
    private String sanitizeFolderName(String name) {
        if (name == null || name.isBlank()) {
            return "Cours_Sans_Titre";
        }
        
        // Remplacer les caractères non autorisés
        return name.replaceAll("[<>:\"/\\\\|?*]", "_")
                  .replaceAll("\\s+", "_")
                  .trim();
    }
    
    /**
     * Supprime un fichier de Google Drive.
     */
    public void deleteFile(String fileId) throws IOException {
        if (fileId != null && !fileId.isBlank()) {
            driveService.files().delete(fileId).execute();
        }
    }
    
    /**
     * Génère un lien partageable vers le dossier d'un cours spécifique.
     */
    public String generateCourseFolderLink(String coursTitle) throws IOException {
        String coursFolderId = getOrCreateCoursFolder(coursTitle);
        return "https://drive.google.com/drive/folders/" + coursFolderId + "?usp=sharing";
    }
    
    /**
     * Upload un cours complet avec tous ses contenus vers Google Drive.
     * 
     * @param cours Le cours à uploader
     * @param chapitres Liste des chapitres du cours
     * @param tds Liste des TDs du cours
     * @param videos Liste des vidéos du cours
     * @return DriveUploadResult avec le lien vers le dossier du cours
     */
    public DriveUploadResult uploadCoursComplet(
            com.educompus.model.Cours cours,
            List<com.educompus.model.Chapitre> chapitres,
            List<com.educompus.model.Td> tds,
            List<com.educompus.model.VideoExplicative> videos) throws IOException {
        
        String coursFolderId = getOrCreateCoursFolder(cours.getTitre());
        
        // Upload du cours principal s'il a un fichier
        if (cours.getImage() != null && !cours.getImage().startsWith("auto:")) {
            uploadCoursFile(cours.getImage(), "Cours_" + cours.getTitre() + ".pdf", cours.getTitre());
        }
        
        // Upload des chapitres
        for (com.educompus.model.Chapitre chapitre : chapitres) {
            if (chapitre.getFichierC() != null && !chapitre.getFichierC().isBlank()) {
                uploadChapitreFile(chapitre.getFichierC(), 
                    "Chapitre_" + chapitre.getOrdre() + "_" + chapitre.getTitre() + ".pdf", 
                    cours.getTitre(), 
                    chapitre.getTitre());
            }
        }
        
        // Upload des TDs
        for (com.educompus.model.Td td : tds) {
            if (td.getFichier() != null && !td.getFichier().isBlank()) {
                uploadTdFile(td.getFichier(), 
                    "TD_" + td.getTitre() + ".pdf", 
                    cours.getTitre(), 
                    td.getTitre());
            }
        }
        
        // Upload des vidéos (créer fichiers texte avec liens)
        for (com.educompus.model.VideoExplicative video : videos) {
            if (video.getUrlVideo() != null && !video.getUrlVideo().isBlank()) {
                // Créer un fichier temporaire avec les informations de la vidéo
                java.io.File tempFile = java.io.File.createTempFile("Video_" + video.getTitre(), ".txt");
                String videoContent = "Titre: " + video.getTitre() + "\n" +
                                    "Description: " + video.getDescription() + "\n" +
                                    "Lien vidéo: " + video.getUrlVideo() + "\n" +
                                    "Type: " + (video.isAIGenerated() ? "Générée par AI" : "Manuelle") + "\n" +
                                    "Statut: " + video.getGenerationStatus();
                
                java.nio.file.Files.writeString(tempFile.toPath(), videoContent);
                
                uploadVideoFile(tempFile.getAbsolutePath(), 
                    "Video_" + video.getTitre() + "_info.txt", 
                    cours.getTitre(), 
                    video.getTitre());
                
                tempFile.delete(); // Nettoyer le fichier temporaire
            }
        }
        
        // Générer le lien partageable vers le dossier du cours
        String shareableLink = generateCourseFolderLink(cours.getTitre());
        
        return new DriveUploadResult(
            coursFolderId,
            shareableLink,
            shareableLink,
            shareableLink,
            0L // Taille totale non calculée pour le moment
        );
    }
    
    /**
     * Vérifie si le service Google Drive est disponible.
     */
    public boolean isAvailable() {
        try {
            driveService.about().get().setFields("user").execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Obtient des informations sur l'utilisation du stockage.
     */
    public DriveStorageInfo getStorageInfo() throws IOException {
        com.google.api.services.drive.model.About about = driveService.about().get()
                .setFields("storageQuota")
                .execute();
        
        com.google.api.services.drive.model.About.StorageQuota quota = about.getStorageQuota();
        
        return new DriveStorageInfo(
            quota.getLimit(),
            quota.getUsage(),
            quota.getUsageInDrive()
        );
    }
    
    /**
     * Résultat d'un upload vers Google Drive.
     */
    public static class DriveUploadResult {
        private final String fileId;
        private final String shareableLink;
        private final String webViewLink;
        private final String webContentLink;
        private final long fileSize;
        
        public DriveUploadResult(String fileId, String shareableLink, String webViewLink, String webContentLink, long fileSize) {
            this.fileId = fileId;
            this.shareableLink = shareableLink;
            this.webViewLink = webViewLink;
            this.webContentLink = webContentLink;
            this.fileSize = fileSize;
        }
        
        public String getFileId() { return fileId; }
        public String getShareableLink() { return shareableLink; }
        public String getWebViewLink() { return webViewLink; }
        public String getWebContentLink() { return webContentLink; }
        public long getFileSize() { return fileSize; }
    }
    
    /**
     * Informations sur l'utilisation du stockage Google Drive.
     */
    public static class DriveStorageInfo {
        private final Long limit;
        private final Long usage;
        private final Long usageInDrive;
        
        public DriveStorageInfo(Long limit, Long usage, Long usageInDrive) {
            this.limit = limit;
            this.usage = usage;
            this.usageInDrive = usageInDrive;
        }
        
        public Long getLimit() { return limit; }
        public Long getUsage() { return usage; }
        public Long getUsageInDrive() { return usageInDrive; }
        
        public double getUsagePercentage() {
            if (limit == null || limit == 0) return 0.0;
            return (usage != null ? usage.doubleValue() : 0.0) / limit.doubleValue() * 100.0;
        }
        
        public String getFormattedUsage() {
            return formatBytes(usage) + " / " + formatBytes(limit) + " (" + String.format("%.1f%%", getUsagePercentage()) + ")";
        }
        
        private String formatBytes(Long bytes) {
            if (bytes == null) return "0 B";
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
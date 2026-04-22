package com.educompus.service;

import com.educompus.util.GoogleDriveAuthUtil;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.Collections;

public class GoogleDriveService {

    private final Drive driveService;

    public GoogleDriveService() {
        NetHttpTransport httpTransport = GoogleDriveAuthUtil.getHttpTransport();
        try {
            this.driveService = new Drive.Builder(
                    httpTransport,
                    com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                    GoogleDriveAuthUtil.getCredentials(httpTransport))
                    .setApplicationName("Educompus")
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Google Drive service", e);
        }
    }

    /**
     * Recherche un dossier par son nom. S'il n'existe pas, le crée.
     * @param folderName Nom du dossier (ex: "Cours - Titre")
     * @return L'ID du dossier
     */
    public String getOrCreateFolder(String folderName) throws IOException {
        String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName.replace("'", "\\'") + "' and trashed=false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }

        // Si le dossier n'existe pas, on le crée
        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");

        File folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();

        return folder.getId();
    }

    /**
     * Uploade un fichier local dans un dossier spécifique.
     */
    public String uploadFileToFolder(java.io.File localFile, String folderId) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(localFile.getName());
        fileMetadata.setParents(Collections.singletonList(folderId));
        
        String mimeType = java.nio.file.Files.probeContentType(localFile.toPath());
        if (mimeType == null) mimeType = "application/octet-stream";

        FileContent mediaContent = new FileContent(mimeType, localFile);

        File file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute();

        System.out.println("Fichier uploadé sur Google Drive (ID: " + file.getId() + ")");
        return file.getId();
    }

    /**
     * Vérifie si un dossier avec ce nom existe déjà sur Drive (pour savoir s'il s'agit d'un cours important).
     */
    public String getFolderIdByName(String folderName) throws IOException {
        String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName.replace("'", "\\'") + "' and trashed=false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }
        return null;
    }
}

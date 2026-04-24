package com.educompus.controller.back;

import com.educompus.model.Chapitre;
import com.educompus.model.Cours;
import com.educompus.model.Td;
import com.educompus.model.VideoExplicative;
import com.educompus.service.ContentManagementService;
import com.educompus.service.GoogleDriveService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

/**
 * Contrôleur pour la gestion de contenu pédagogique avec intégration Google Drive.
 */
public final class BackContentManagementController {

    @FXML private TabPane mainTabPane;
    
    // Onglet Cours
    @FXML private TextField coursTitle;
    @FXML private TextArea coursDescription;
    @FXML private ComboBox<String> coursNiveau;
    @FXML private ComboBox<String> coursDomaine;
    @FXML private TextField coursFormateur;
    @FXML private Spinner<Integer> coursDuree;
    @FXML private CheckBox coursImportant;
    @FXML private Label coursFileLabel;
    @FXML private Button coursSelectFileBtn;
    @FXML private Button coursCreateBtn;
    @FXML private ProgressIndicator coursProgressIndicator;
    @FXML private Label coursStatusLabel;
    
    // Onglet Chapitres
    @FXML private ComboBox<Cours> chapitreCoursCombo;
    @FXML private TextField chapitreTitle;
    @FXML private TextArea chapitreDescription;
    @FXML private Spinner<Integer> chapitreOrdre;
    @FXML private CheckBox chapitreImportant;
    @FXML private Label chapitreFileLabel;
    @FXML private Button chapitreSelectFileBtn;
    @FXML private Button chapitreCreateBtn;
    @FXML private ProgressIndicator chapitreProgressIndicator;
    @FXML private Label chapitreStatusLabel;
    
    // Onglet TDs
    @FXML private ComboBox<Cours> tdCoursCombo;
    @FXML private ComboBox<Chapitre> tdChapitreCombo;
    @FXML private TextField tdTitle;
    @FXML private TextArea tdDescription;
    @FXML private CheckBox tdImportant;
    @FXML private Label tdFileLabel;
    @FXML private Button tdSelectFileBtn;
    @FXML private Button tdCreateBtn;
    @FXML private ProgressIndicator tdProgressIndicator;
    @FXML private Label tdStatusLabel;
    
    // Onglet Vidéos
    @FXML private ComboBox<Cours> videoCoursCombo;
    @FXML private ComboBox<Chapitre> videoChapitreCombo;
    @FXML private TextField videoTitle;
    @FXML private TextArea videoDescription;
    @FXML private TextField videoUrl;
    @FXML private CheckBox videoImportant;
    @FXML private Label videoFileLabel;
    @FXML private Button videoSelectFileBtn;
    @FXML private Button videoCreateBtn;
    @FXML private ProgressIndicator videoProgressIndicator;
    @FXML private Label videoStatusLabel;
    
    // Onglet Statistiques Google Drive
    @FXML private Label driveStatusLabel;
    @FXML private Label driveUsageLabel;
    @FXML private ProgressBar driveUsageBar;
    @FXML private Button driveRefreshBtn;
    @FXML private TextArea driveStatsArea;
    
    private ContentManagementService contentService;
    private File selectedCoursFile;
    private File selectedChapitreFile;
    private File selectedTdFile;
    private File selectedVideoFile;
    
    @FXML
    private void initialize() {
        try {
            contentService = new ContentManagementService();
            setupUI();
            loadInitialData();
            checkDriveStatus();
        } catch (IOException | GeneralSecurityException e) {
            showError("Erreur d'initialisation", "Impossible d'initialiser le service Google Drive: " + e.getMessage());
        }
    }
    
    /**
     * Configure l'interface utilisateur.
     */
    private void setupUI() {
        // Configuration des ComboBox
        coursNiveau.getItems().addAll("Débutant", "Intermédiaire", "Avancé", "Expert");
        coursDomaine.getItems().addAll("Informatique", "Mathématiques", "Physique", "Chimie", "Biologie", "Économie", "Gestion");
        
        // Configuration des Spinners
        coursDuree.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 10));
        chapitreOrdre.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 1));
        
        // Masquer les indicateurs de progression initialement
        coursProgressIndicator.setVisible(false);
        chapitreProgressIndicator.setVisible(false);
        tdProgressIndicator.setVisible(false);
        videoProgressIndicator.setVisible(false);
        
        // Configuration des listeners pour les ComboBox dépendants
        chapitreCoursCombo.setOnAction(e -> updateChapitresList());
        tdCoursCombo.setOnAction(e -> updateTdChapitresList());
        videoCoursCombo.setOnAction(e -> updateVideoChapitresList());
    }
    
    /**
     * Charge les données initiales.
     */
    private void loadInitialData() {
        // Charger la liste des cours dans les ComboBox
        try {
            var cours = contentService.repository.listCours("");
            chapitreCoursCombo.getItems().setAll(cours);
            tdCoursCombo.getItems().setAll(cours);
            videoCoursCombo.getItems().setAll(cours);
        } catch (Exception e) {
            showError("Erreur de chargement", "Impossible de charger la liste des cours: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // GESTION DES COURS
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    @FXML
    private void onCoursSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier de cours");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        
        Window window = coursSelectFileBtn.getScene().getWindow();
        selectedCoursFile = fileChooser.showOpenDialog(window);
        
        if (selectedCoursFile != null) {
            coursFileLabel.setText(selectedCoursFile.getName());
        }
    }
    
    @FXML
    private void onCoursCreate() {
        if (!validateCoursForm()) return;
        
        Cours cours = new Cours();
        cours.setTitre(coursTitle.getText().trim());
        cours.setDescription(coursDescription.getText().trim());
        cours.setNiveau(coursNiveau.getValue());
        cours.setDomaine(coursDomaine.getValue());
        cours.setNomFormateur(coursFormateur.getText().trim());
        cours.setDureeTotaleHeures(coursDuree.getValue());
        cours.setImportant(coursImportant.isSelected());
        
        boolean important = coursImportant.isSelected();
        String filePath = selectedCoursFile != null ? selectedCoursFile.getAbsolutePath() : null;
        
        // Exécution asynchrone
        executeAsyncOperation(
            () -> contentService.createCours(cours, important, filePath),
            coursProgressIndicator,
            coursStatusLabel,
            "Cours créé avec succès",
            this::clearCoursForm
        );
    }
    
    private boolean validateCoursForm() {
        if (coursTitle.getText() == null || coursTitle.getText().trim().isEmpty()) {
            showError("Validation", "Le titre du cours est obligatoire.");
            return false;
        }
        if (coursNiveau.getValue() == null) {
            showError("Validation", "Veuillez sélectionner un niveau.");
            return false;
        }
        if (coursDomaine.getValue() == null) {
            showError("Validation", "Veuillez sélectionner un domaine.");
            return false;
        }
        if (coursImportant.isSelected() && selectedCoursFile == null) {
            showError("Validation", "Un fichier est requis pour les cours importants.");
            return false;
        }
        return true;
    }
    
    private void clearCoursForm() {
        coursTitle.clear();
        coursDescription.clear();
        coursNiveau.setValue(null);
        coursDomaine.setValue(null);
        coursFormateur.clear();
        coursDuree.getValueFactory().setValue(10);
        coursImportant.setSelected(false);
        coursFileLabel.setText("Aucun fichier sélectionné");
        selectedCoursFile = null;
        loadInitialData(); // Recharger les listes
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // GESTION DES CHAPITRES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    @FXML
    private void onChapitreSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier de chapitre");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        
        Window window = chapitreSelectFileBtn.getScene().getWindow();
        selectedChapitreFile = fileChooser.showOpenDialog(window);
        
        if (selectedChapitreFile != null) {
            chapitreFileLabel.setText(selectedChapitreFile.getName());
        }
    }
    
    @FXML
    private void onChapitreCreate() {
        if (!validateChapitreForm()) return;
        
        Chapitre chapitre = new Chapitre();
        chapitre.setTitre(chapitreTitle.getText().trim());
        chapitre.setDescription(chapitreDescription.getText().trim());
        chapitre.setOrdre(chapitreOrdre.getValue());
        chapitre.setCoursId(chapitreCoursCombo.getValue().getId());
        chapitre.setImportant(chapitreImportant.isSelected());
        
        boolean important = chapitreImportant.isSelected();
        String filePath = selectedChapitreFile != null ? selectedChapitreFile.getAbsolutePath() : null;
        
        executeAsyncOperation(
            () -> contentService.createChapitre(chapitre, important, filePath),
            chapitreProgressIndicator,
            chapitreStatusLabel,
            "Chapitre créé avec succès",
            this::clearChapitreForm
        );
    }
    
    private boolean validateChapitreForm() {
        if (chapitreTitle.getText() == null || chapitreTitle.getText().trim().isEmpty()) {
            showError("Validation", "Le titre du chapitre est obligatoire.");
            return false;
        }
        if (chapitreCoursCombo.getValue() == null) {
            showError("Validation", "Veuillez sélectionner un cours.");
            return false;
        }
        if (chapitreImportant.isSelected() && selectedChapitreFile == null) {
            showError("Validation", "Un fichier est requis pour les chapitres importants.");
            return false;
        }
        return true;
    }
    
    private void clearChapitreForm() {
        chapitreTitle.clear();
        chapitreDescription.clear();
        chapitreOrdre.getValueFactory().setValue(1);
        chapitreCoursCombo.setValue(null);
        chapitreImportant.setSelected(false);
        chapitreFileLabel.setText("Aucun fichier sélectionné");
        selectedChapitreFile = null;
    }
    
    private void updateChapitresList() {
        // Cette méthode peut être étendue pour filtrer les chapitres par cours
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // GESTION DES TDs
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    @FXML
    private void onTdSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier de TD");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        
        Window window = tdSelectFileBtn.getScene().getWindow();
        selectedTdFile = fileChooser.showOpenDialog(window);
        
        if (selectedTdFile != null) {
            tdFileLabel.setText(selectedTdFile.getName());
        }
    }
    
    @FXML
    private void onTdCreate() {
        if (!validateTdForm()) return;
        
        Td td = new Td();
        td.setTitre(tdTitle.getText().trim());
        td.setDescription(tdDescription.getText().trim());
        td.setCoursId(tdCoursCombo.getValue().getId());
        if (tdChapitreCombo.getValue() != null) {
            td.setChapitreId(tdChapitreCombo.getValue().getId());
        }
        td.setImportant(tdImportant.isSelected());
        
        boolean important = tdImportant.isSelected();
        String filePath = selectedTdFile != null ? selectedTdFile.getAbsolutePath() : null;
        
        executeAsyncOperation(
            () -> contentService.createTd(td, important, filePath),
            tdProgressIndicator,
            tdStatusLabel,
            "TD créé avec succès",
            this::clearTdForm
        );
    }
    
    private boolean validateTdForm() {
        if (tdTitle.getText() == null || tdTitle.getText().trim().isEmpty()) {
            showError("Validation", "Le titre du TD est obligatoire.");
            return false;
        }
        if (tdCoursCombo.getValue() == null) {
            showError("Validation", "Veuillez sélectionner un cours.");
            return false;
        }
        if (tdImportant.isSelected() && selectedTdFile == null) {
            showError("Validation", "Un fichier est requis pour les TDs importants.");
            return false;
        }
        return true;
    }
    
    private void clearTdForm() {
        tdTitle.clear();
        tdDescription.clear();
        tdCoursCombo.setValue(null);
        tdChapitreCombo.setValue(null);
        tdImportant.setSelected(false);
        tdFileLabel.setText("Aucun fichier sélectionné");
        selectedTdFile = null;
    }
    
    private void updateTdChapitresList() {
        Cours selectedCours = tdCoursCombo.getValue();
        if (selectedCours != null) {
            try {
                var chapitres = contentService.repository.listChapitresByCoursId(selectedCours.getId());
                tdChapitreCombo.getItems().setAll(chapitres);
            } catch (Exception e) {
                showError("Erreur", "Impossible de charger les chapitres: " + e.getMessage());
            }
        } else {
            tdChapitreCombo.getItems().clear();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // GESTION DES VIDÉOS
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    @FXML
    private void onVideoSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier vidéo");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Fichiers vidéo", "*.mp4", "*.avi", "*.mov", "*.wmv"),
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        
        Window window = videoSelectFileBtn.getScene().getWindow();
        selectedVideoFile = fileChooser.showOpenDialog(window);
        
        if (selectedVideoFile != null) {
            videoFileLabel.setText(selectedVideoFile.getName());
        }
    }
    
    @FXML
    private void onVideoCreate() {
        if (!validateVideoForm()) return;
        
        VideoExplicative video = new VideoExplicative();
        video.setTitre(videoTitle.getText().trim());
        video.setDescription(videoDescription.getText().trim());
        video.setUrlVideo(videoUrl.getText().trim());
        video.setCoursId(videoCoursCombo.getValue().getId());
        if (videoChapitreCombo.getValue() != null) {
            video.setChapitreId(videoChapitreCombo.getValue().getId());
        }
        video.setImportant(videoImportant.isSelected());
        
        boolean important = videoImportant.isSelected();
        String filePath = selectedVideoFile != null ? selectedVideoFile.getAbsolutePath() : null;
        
        executeAsyncOperation(
            () -> contentService.createVideo(video, important, filePath),
            videoProgressIndicator,
            videoStatusLabel,
            "Vidéo créée avec succès",
            this::clearVideoForm
        );
    }
    
    private boolean validateVideoForm() {
        if (videoTitle.getText() == null || videoTitle.getText().trim().isEmpty()) {
            showError("Validation", "Le titre de la vidéo est obligatoire.");
            return false;
        }
        if (videoCoursCombo.getValue() == null) {
            showError("Validation", "Veuillez sélectionner un cours.");
            return false;
        }
        if (videoImportant.isSelected() && selectedVideoFile == null && 
            (videoUrl.getText() == null || videoUrl.getText().trim().isEmpty())) {
            showError("Validation", "Un fichier ou une URL est requis pour les vidéos importantes.");
            return false;
        }
        return true;
    }
    
    private void clearVideoForm() {
        videoTitle.clear();
        videoDescription.clear();
        videoUrl.clear();
        videoCoursCombo.setValue(null);
        videoChapitreCombo.setValue(null);
        videoImportant.setSelected(false);
        videoFileLabel.setText("Aucun fichier sélectionné");
        selectedVideoFile = null;
    }
    
    private void updateVideoChapitresList() {
        Cours selectedCours = videoCoursCombo.getValue();
        if (selectedCours != null) {
            try {
                var chapitres = contentService.repository.listChapitresByCoursId(selectedCours.getId());
                videoChapitreCombo.getItems().setAll(chapitres);
            } catch (Exception e) {
                showError("Erreur", "Impossible de charger les chapitres: " + e.getMessage());
            }
        } else {
            videoChapitreCombo.getItems().clear();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // GESTION GOOGLE DRIVE
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    @FXML
    private void onDriveRefresh() {
        checkDriveStatus();
    }
    
    private void checkDriveStatus() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    driveRefreshBtn.setDisable(true);
                    driveStatusLabel.setText("Vérification du statut Google Drive...");
                });
                
                boolean available = contentService.isDriveAvailable();
                
                Platform.runLater(() -> {
                    if (available) {
                        driveStatusLabel.setText("✅ Google Drive connecté");
                        driveStatusLabel.setStyle("-fx-text-fill: green;");
                        loadDriveStats();
                    } else {
                        driveStatusLabel.setText("❌ Google Drive non disponible");
                        driveStatusLabel.setStyle("-fx-text-fill: red;");
                        driveUsageLabel.setText("Non disponible");
                        driveUsageBar.setProgress(0);
                        driveStatsArea.setText("Google Drive n'est pas accessible.\nVérifiez votre connexion et vos credentials.");
                    }
                    driveRefreshBtn.setDisable(false);
                });
                
                return null;
            }
        };
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void loadDriveStats() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    GoogleDriveService.DriveStorageInfo info = contentService.getStorageInfo();
                    
                    Platform.runLater(() -> {
                        driveUsageLabel.setText(info.getFormattedUsage());
                        driveUsageBar.setProgress(info.getUsagePercentage() / 100.0);
                        
                        StringBuilder stats = new StringBuilder();
                        stats.append("📊 STATISTIQUES GOOGLE DRIVE\n\n");
                        stats.append("Utilisation: ").append(info.getFormattedUsage()).append("\n");
                        stats.append("Pourcentage: ").append(String.format("%.1f%%", info.getUsagePercentage())).append("\n\n");
                        stats.append("💡 CONSEILS:\n");
                        stats.append("• Marquez comme 'important' uniquement les contenus essentiels\n");
                        stats.append("• Les contenus non importants restent en base de données locale\n");
                        stats.append("• Optimisez l'espace en supprimant les anciens fichiers inutiles");
                        
                        driveStatsArea.setText(stats.toString());
                    });
                    
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        driveStatsArea.setText("Erreur lors de la récupération des statistiques: " + e.getMessage());
                    });
                }
                
                return null;
            }
        };
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Exécute une opération de manière asynchrone avec feedback visuel.
     */
    private void executeAsyncOperation(
            java.util.function.Supplier<ContentManagementService.ContentOperationResult> operation,
            ProgressIndicator progressIndicator,
            Label statusLabel,
            String successMessage,
            Runnable onSuccess) {
        
        Task<ContentManagementService.ContentOperationResult> task = new Task<>() {
            @Override
            protected ContentManagementService.ContentOperationResult call() throws Exception {
                return operation.get();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    ContentManagementService.ContentOperationResult result = getValue();
                    
                    if (result.isSuccess()) {
                        statusLabel.setText("✅ " + successMessage);
                        statusLabel.setStyle("-fx-text-fill: green;");
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                        
                        // Afficher les détails si upload vers Drive
                        if (result.isUploadedToDrive()) {
                            showInfo("Succès", result.toString());
                        }
                    } else {
                        statusLabel.setText("❌ " + result.getMessage());
                        statusLabel.setStyle("-fx-text-fill: red;");
                        showError("Erreur", result.getMessage());
                    }
                    
                    // Effacer le message après 5 secondes
                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
                    pause.setOnFinished(e -> statusLabel.setText(""));
                    pause.play();
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    statusLabel.setText("❌ Erreur inattendue");
                    statusLabel.setStyle("-fx-text-fill: red;");
                    showError("Erreur", "Une erreur inattendue s'est produite: " + getException().getMessage());
                });
            }
        };
        
        progressIndicator.setVisible(true);
        statusLabel.setText("Traitement en cours...");
        statusLabel.setStyle("-fx-text-fill: blue;");
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
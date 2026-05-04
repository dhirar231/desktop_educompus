package com.educompus.controller.back;

import com.educompus.model.Chapitre;
import com.educompus.model.VideoExplicative;
import com.educompus.model.ParametresGeneration;
import com.educompus.service.ChapitreService;
import com.educompus.service.VideoExplicatifService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur pour la gestion des vidéos générées par IA.
 */
public class BackVideoAIController implements Initializable {

    @FXML private ComboBox<Chapitre> comboChapitres;
    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private Spinner<Integer> spinnerDuree;
    @FXML private ComboBox<String> comboLangue;
    @FXML private ComboBox<String> comboQualite;
    @FXML private ComboBox<String> comboVoix;
    @FXML private ComboBox<String> comboStyle;
    @FXML private Button btnGenerer;
    @FXML private Button btnAnnuler;
    @FXML private ProgressBar progressGeneration;
    @FXML private Label lblStatut;
    @FXML private TextArea txtScript;
    
    // Table des vidéos
    @FXML private TableView<VideoExplicative> tableVideos;
    @FXML private TableColumn<VideoExplicative, Integer> colId;
    @FXML private TableColumn<VideoExplicative, String> colTitre;
    @FXML private TableColumn<VideoExplicative, String> colChapitre;
    @FXML private TableColumn<VideoExplicative, String> colStatut;
    @FXML private TableColumn<VideoExplicative, String> colDateCreation;
    @FXML private TableColumn<VideoExplicative, Boolean> colIA;
    
    // Boutons d'action
    @FXML private Button btnVoir;
    @FXML private Button btnSupprimer;
    @FXML private Button btnRafraichir;
    @FXML private TextField txtRecherche;

    private VideoExplicatifService videoService;
    private ChapitreService chapitreService;
    private ObservableList<VideoExplicative> videosData;
    private CompletableFuture<VideoExplicative> generationEnCours;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        videoService = new VideoExplicatifService();
        chapitreService = new ChapitreService();
        videosData = FXCollections.observableArrayList();

        initialiserComposants();
        configurerTable();
        chargerDonnees();
    }

    private void initialiserComposants() {
        // Configuration du spinner de durée
        spinnerDuree.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 5));
        
        // Configuration des combos
        comboLangue.setItems(FXCollections.observableArrayList("fr", "en", "es", "de", "it"));
        comboLangue.setValue("fr");
        
        comboQualite.setItems(FXCollections.observableArrayList("HD", "4K", "Standard"));
        comboQualite.setValue("HD");
        
        comboVoix.setItems(FXCollections.observableArrayList("neutre", "masculine", "feminine"));
        comboVoix.setValue("neutre");
        
        comboStyle.setItems(FXCollections.observableArrayList(
            "pédagogique", "professionnel", "décontracté", "académique", "vulgarisation"
        ));
        comboStyle.setValue("pédagogique");

        // État initial
        progressGeneration.setVisible(false);
        btnAnnuler.setDisable(true);
        lblStatut.setText("Prêt");
        
        // Événements
        btnGenerer.setOnAction(e -> genererVideo());
        btnAnnuler.setOnAction(e -> annulerGeneration());
        btnVoir.setOnAction(e -> voirVideo());
        btnSupprimer.setOnAction(e -> supprimerVideo());
        btnRafraichir.setOnAction(e -> rafraichirListe());
        txtRecherche.textProperty().addListener((obs, old, nouveau) -> filtrerVideos(nouveau));
        
        comboChapitres.setOnAction(e -> chargerInfosChapitre());
    }

    private void configurerTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colChapitre.setCellValueFactory(new PropertyValueFactory<>("chapitreTitre"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("generationStatus"));
        colDateCreation.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        colIA.setCellValueFactory(new PropertyValueFactory<>("AIGenerated"));
        
        // Formatage personnalisé pour la colonne IA
        colIA.setCellFactory(column -> new TableCell<VideoExplicative, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item ? "IA" : "Manuel");
                    setStyle(item ? "-fx-text-fill: blue;" : "-fx-text-fill: green;");
                }
            }
        });
        
        // Formatage du statut
        colStatut.setCellFactory(column -> new TableCell<VideoExplicative, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "COMPLETED" -> setStyle("-fx-text-fill: green;");
                        case "PROCESSING" -> setStyle("-fx-text-fill: orange;");
                        case "ERROR" -> setStyle("-fx-text-fill: red;");
                        default -> setStyle("");
                    }
                }
            }
        });

        tableVideos.setItems(videosData);
        
        // Sélection
        tableVideos.getSelectionModel().selectedItemProperty().addListener((obs, old, nouveau) -> {
            boolean hasSelection = nouveau != null;
            btnVoir.setDisable(!hasSelection);
            btnSupprimer.setDisable(!hasSelection);
            
            if (nouveau != null && nouveau.getAiScript() != null) {
                txtScript.setText(nouveau.getAiScript());
            } else {
                txtScript.clear();
            }
        });
    }

    private void chargerDonnees() {
        // Charger les chapitres
        Task<List<Chapitre>> taskChapitres = new Task<>() {
            @Override
            protected List<Chapitre> call() {
                return chapitreService.listerTous("");
            }
        };
        
        taskChapitres.setOnSucceeded(e -> {
            List<Chapitre> chapitres = taskChapitres.getValue();
            comboChapitres.setItems(FXCollections.observableArrayList(chapitres));
        });
        
        taskChapitres.setOnFailed(e -> {
            afficherErreur("Erreur lors du chargement des chapitres", taskChapitres.getException());
        });
        
        new Thread(taskChapitres).start();
        
        // Charger les vidéos
        rafraichirListe();
    }

    private void chargerInfosChapitre() {
        Chapitre chapitre = comboChapitres.getValue();
        if (chapitre != null) {
            txtTitre.setText("Vidéo IA: " + chapitre.getTitre());
            txtDescription.setText("Vidéo générée automatiquement pour: " + chapitre.getDescription());
        }
    }

    private void genererVideo() {
        Chapitre chapitre = comboChapitres.getValue();
        if (chapitre == null) {
            afficherErreur("Erreur", "Veuillez sélectionner un chapitre.");
            return;
        }

        // Créer les paramètres
        ParametresGeneration parametres = new ParametresGeneration();
        parametres.setDureeMinutes(spinnerDuree.getValue());
        parametres.setLangue(comboLangue.getValue());
        parametres.setQualite(comboQualite.getValue());
        parametres.setVoixType(comboVoix.getValue());
        parametres.setStyleNarration(comboStyle.getValue());

        // Démarrer la génération
        demarrerGeneration(chapitre.getId(), parametres);
    }

    private void demarrerGeneration(int chapitreId, ParametresGeneration parametres) {
        // Interface en mode génération
        btnGenerer.setDisable(true);
        btnAnnuler.setDisable(false);
        progressGeneration.setVisible(true);
        progressGeneration.setProgress(-1); // Indéterminé
        lblStatut.setText("Génération en cours...");

        // Lancer la génération asynchrone
        generationEnCours = videoService.genererVideoAsync(chapitreId, parametres);
        
        generationEnCours.whenComplete((video, throwable) -> {
            Platform.runLater(() -> {
                // Restaurer l'interface
                btnGenerer.setDisable(false);
                btnAnnuler.setDisable(true);
                progressGeneration.setVisible(false);
                
                if (throwable != null) {
                    lblStatut.setText("Erreur lors de la génération");
                    afficherErreur("Erreur de génération", throwable);
                } else {
                    lblStatut.setText("Vidéo générée avec succès !");
                    rafraichirListe();
                    
                    // Sélectionner la nouvelle vidéo
                    tableVideos.getSelectionModel().select(video);
                    
                    afficherInfo("Succès", "La vidéo a été générée avec succès !");
                }
            });
        });
    }

    private void annulerGeneration() {
        if (generationEnCours != null && !generationEnCours.isDone()) {
            generationEnCours.cancel(true);
            
            btnGenerer.setDisable(false);
            btnAnnuler.setDisable(true);
            progressGeneration.setVisible(false);
            lblStatut.setText("Génération annulée");
        }
    }

    private void voirVideo() {
        VideoExplicative video = tableVideos.getSelectionModel().getSelectedItem();
        if (video != null && video.getUrlVideo() != null) {
            try {
                // Ouvrir l'URL dans le navigateur par défaut
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(video.getUrlVideo()));
            } catch (Exception e) {
                afficherErreur("Erreur", "Impossible d'ouvrir la vidéo: " + e.getMessage());
            }
        }
    }

    private void supprimerVideo() {
        VideoExplicative video = tableVideos.getSelectionModel().getSelectedItem();
        if (video != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Supprimer la vidéo");
            confirmation.setContentText("Êtes-vous sûr de vouloir supprimer cette vidéo ?");
            
            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    videoService.supprimer(video.getId());
                    rafraichirListe();
                    afficherInfo("Succès", "Vidéo supprimée avec succès.");
                } catch (Exception e) {
                    afficherErreur("Erreur", "Impossible de supprimer la vidéo: " + e.getMessage());
                }
            }
        }
    }

    private void rafraichirListe() {
        Task<List<VideoExplicative>> task = new Task<>() {
            @Override
            protected List<VideoExplicative> call() {
                return videoService.listerToutes("");
            }
        };
        
        task.setOnSucceeded(e -> {
            videosData.clear();
            videosData.addAll(task.getValue());
        });
        
        task.setOnFailed(e -> {
            afficherErreur("Erreur", "Impossible de charger les vidéos: " + task.getException().getMessage());
        });
        
        new Thread(task).start();
    }

    private void filtrerVideos(String recherche) {
        if (recherche == null || recherche.trim().isEmpty()) {
            rafraichirListe();
            return;
        }
        
        Task<List<VideoExplicative>> task = new Task<>() {
            @Override
            protected List<VideoExplicative> call() {
                return videoService.listerToutes(recherche);
            }
        };
        
        task.setOnSucceeded(e -> {
            videosData.clear();
            videosData.addAll(task.getValue());
        });
        
        new Thread(task).start();
    }

    private void afficherErreur(String titre, Throwable erreur) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText("Une erreur s'est produite");
        alert.setContentText(erreur.getMessage());
        alert.showAndWait();
    }

    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void afficherInfo(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
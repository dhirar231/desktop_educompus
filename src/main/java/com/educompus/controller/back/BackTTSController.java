package com.educompus.controller.back;

import com.educompus.model.Chapitre;
import com.educompus.model.VideoExplicative;
import com.educompus.service.ChapitreService;
import com.educompus.service.GoogleTTSService;
import com.educompus.service.GoogleTTSService.ParametresTTS;
import com.educompus.service.GoogleTTSService.ResultatTTS;
import com.educompus.service.GoogleTTSService.VoixFrancaise;
import com.educompus.service.VideoExplicatifService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur pour la conversion texte-vers-parole (TTS).
 */
public class BackTTSController implements Initializable {

    @FXML private ComboBox<Chapitre> comboChapitres;
    @FXML private ComboBox<VideoExplicative> comboVideos;
    @FXML private TextArea txtTexte;
    @FXML private ComboBox<VoixFrancaise> comboVoix;
    @FXML private Slider sliderVitesse;
    @FXML private Label lblVitesse;
    @FXML private Slider sliderPitch;
    @FXML private Label lblPitch;
    @FXML private Slider sliderVolume;
    @FXML private Label lblVolume;
    @FXML private ComboBox<String> comboFormat;
    
    @FXML private Button btnConvertir;
    @FXML private Button btnTester;
    @FXML private Button btnLire;
    @FXML private Button btnArreter;
    @FXML private ProgressBar progressConversion;
    @FXML private Label lblStatut;
    @FXML private Label lblResultat;
    
    // Historique
    @FXML private ListView<String> listeFichiers;
    @FXML private Button btnSupprimerFichier;
    @FXML private Button btnOuvrirDossier;

    private ChapitreService chapitreService;
    private VideoExplicatifService videoService;
    private MediaPlayer mediaPlayer;
    private CompletableFuture<ResultatTTS> conversionEnCours;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chapitreService = new ChapitreService();
        videoService = new VideoExplicatifService();

        initialiserComposants();
        chargerDonnees();
        configurerEvenements();
    }

    private void initialiserComposants() {
        // Configuration des voix
        comboVoix.setItems(FXCollections.observableArrayList(VoixFrancaise.values()));
        comboVoix.setValue(VoixFrancaise.FEMALE_A);
        
        // Configuration des formats
        comboFormat.setItems(FXCollections.observableArrayList("MP3", "WAV", "OGG"));
        comboFormat.setValue("MP3");

        // Configuration des sliders
        sliderVitesse.setMin(0.25);
        sliderVitesse.setMax(2.0);
        sliderVitesse.setValue(1.0);
        
        sliderPitch.setMin(-10.0);
        sliderPitch.setMax(10.0);
        sliderPitch.setValue(0.0);
        
        sliderVolume.setMin(-10.0);
        sliderVolume.setMax(10.0);
        sliderVolume.setValue(0.0);

        // État initial
        progressConversion.setVisible(false);
        btnLire.setDisable(true);
        btnArreter.setDisable(true);
        lblStatut.setText("Prêt");
        
        // Texte par défaut
        txtTexte.setText("Bonjour ! Bienvenue dans cette démonstration de synthèse vocale. " +
                        "Vous pouvez saisir votre propre texte ici, ou charger le script d'un chapitre existant.");
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
        
        new Thread(taskChapitres).start();
        
        // Charger l'historique des fichiers
        rafraichirHistorique();
    }

    private void configurerEvenements() {
        // Sliders avec mise à jour des labels
        sliderVitesse.valueProperty().addListener((obs, old, nouveau) -> {
            lblVitesse.setText(String.format("%.2fx", nouveau.doubleValue()));
        });
        
        sliderPitch.valueProperty().addListener((obs, old, nouveau) -> {
            lblPitch.setText(String.format("%.1f", nouveau.doubleValue()));
        });
        
        sliderVolume.valueProperty().addListener((obs, old, nouveau) -> {
            lblVolume.setText(String.format("%.1f dB", nouveau.doubleValue()));
        });

        // Sélection de chapitre
        comboChapitres.setOnAction(e -> chargerVideosChapitres());
        
        // Sélection de vidéo
        comboVideos.setOnAction(e -> chargerScriptVideo());

        // Boutons
        btnConvertir.setOnAction(e -> convertirTexte());
        btnTester.setOnAction(e -> testerConfiguration());
        btnLire.setOnAction(e -> lireFichier());
        btnArreter.setOnAction(e -> arreterLecture());
        btnSupprimerFichier.setOnAction(e -> supprimerFichier());
        btnOuvrirDossier.setOnAction(e -> ouvrirDossierAudio());

        // Sélection dans l'historique
        listeFichiers.getSelectionModel().selectedItemProperty().addListener((obs, old, nouveau) -> {
            btnLire.setDisable(nouveau == null);
            btnSupprimerFichier.setDisable(nouveau == null);
        });
    }

    private void chargerVideosChapitres() {
        Chapitre chapitre = comboChapitres.getValue();
        if (chapitre != null) {
            Task<List<VideoExplicative>> task = new Task<>() {
                @Override
                protected List<VideoExplicative> call() {
                    return videoService.listerVideosParChapitre(chapitre.getId());
                }
            };
            
            task.setOnSucceeded(e -> {
                List<VideoExplicative> videos = task.getValue();
                comboVideos.setItems(FXCollections.observableArrayList(videos));
                
                // Charger aussi la description du chapitre
                if (txtTexte.getText().isEmpty() || txtTexte.getText().startsWith("Bonjour !")) {
                    txtTexte.setText(chapitre.getDescription());
                }
            });
            
            new Thread(task).start();
        }
    }

    private void chargerScriptVideo() {
        VideoExplicative video = comboVideos.getValue();
        if (video != null && video.getAiScript() != null) {
            txtTexte.setText(video.getAiScript());
        }
    }

    private void convertirTexte() {
        String texte = txtTexte.getText().trim();
        if (texte.isEmpty()) {
            afficherErreur("Erreur", "Veuillez saisir un texte à convertir.");
            return;
        }

        // Créer les paramètres
        ParametresTTS parametres = new ParametresTTS();
        parametres.setVoix(comboVoix.getValue());
        parametres.setVitesse(sliderVitesse.getValue());
        parametres.setPitch(sliderPitch.getValue());
        parametres.setVolumeGain(sliderVolume.getValue());
        parametres.setFormat(comboFormat.getValue());

        // Démarrer la conversion
        demarrerConversion(texte, parametres);
    }

    private void demarrerConversion(String texte, ParametresTTS parametres) {
        // Interface en mode conversion
        btnConvertir.setDisable(true);
        progressConversion.setVisible(true);
        progressConversion.setProgress(-1); // Indéterminé
        lblStatut.setText("Conversion en cours...");
        lblResultat.setText("");

        // Lancer la conversion asynchrone
        conversionEnCours = GoogleTTSService.convertirTexteAsync(texte, parametres);
        
        conversionEnCours.whenComplete((resultat, throwable) -> {
            Platform.runLater(() -> {
                // Restaurer l'interface
                btnConvertir.setDisable(false);
                progressConversion.setVisible(false);
                
                if (throwable != null) {
                    lblStatut.setText("Erreur lors de la conversion");
                    afficherErreur("Erreur TTS", throwable.getMessage());
                } else if (resultat.isSucces()) {
                    lblStatut.setText("Conversion réussie !");
                    lblResultat.setText(String.format("Fichier: %s | Taille: %s | Durée: %s", 
                            new File(resultat.getCheminFichier()).getName(),
                            resultat.getTailleFormatee(),
                            resultat.getDureeFormatee()));
                    
                    rafraichirHistorique();
                    btnLire.setDisable(false);
                    
                    afficherInfo("Succès", "Audio généré avec succès !\n" + 
                               "Fichier: " + resultat.getCheminFichier());
                } else {
                    lblStatut.setText("Erreur de conversion");
                    afficherErreur("Erreur TTS", resultat.getMessageErreur());
                }
            });
        });
    }

    private void testerConfiguration() {
        lblStatut.setText("Test de configuration...");
        
        Task<ResultatTTS> task = new Task<>() {
            @Override
            protected ResultatTTS call() {
                return GoogleTTSService.testerConfiguration();
            }
        };
        
        task.setOnSucceeded(e -> {
            ResultatTTS resultat = task.getValue();
            if (resultat.isSucces()) {
                lblStatut.setText("Configuration OK !");
                afficherInfo("Test réussi", "La configuration TTS fonctionne correctement.");
                rafraichirHistorique();
            } else {
                lblStatut.setText("Erreur de configuration");
                afficherErreur("Test échoué", resultat.getMessageErreur());
            }
        });
        
        task.setOnFailed(e -> {
            lblStatut.setText("Erreur de test");
            afficherErreur("Erreur", "Impossible de tester la configuration: " + task.getException().getMessage());
        });
        
        new Thread(task).start();
    }

    private void lireFichier() {
        String fichierSelectionne = listeFichiers.getSelectionModel().getSelectedItem();
        if (fichierSelectionne != null) {
            try {
                // Arrêter la lecture précédente
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                }
                
                // Créer le chemin complet
                String cheminComplet = "audio/generated/" + fichierSelectionne;
                File fichier = new File(cheminComplet);
                
                if (fichier.exists()) {
                    Media media = new Media(fichier.toURI().toString());
                    mediaPlayer = new MediaPlayer(media);
                    
                    mediaPlayer.setOnReady(() -> {
                        btnLire.setDisable(true);
                        btnArreter.setDisable(false);
                        lblStatut.setText("Lecture en cours...");
                        mediaPlayer.play();
                    });
                    
                    mediaPlayer.setOnEndOfMedia(() -> {
                        btnLire.setDisable(false);
                        btnArreter.setDisable(true);
                        lblStatut.setText("Lecture terminée");
                    });
                    
                    mediaPlayer.setOnError(() -> {
                        btnLire.setDisable(false);
                        btnArreter.setDisable(true);
                        lblStatut.setText("Erreur de lecture");
                        afficherErreur("Erreur", "Impossible de lire le fichier audio.");
                    });
                } else {
                    afficherErreur("Erreur", "Fichier introuvable: " + cheminComplet);
                }
                
            } catch (Exception e) {
                afficherErreur("Erreur", "Impossible de lire le fichier: " + e.getMessage());
            }
        }
    }

    private void arreterLecture() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            btnLire.setDisable(false);
            btnArreter.setDisable(true);
            lblStatut.setText("Lecture arrêtée");
        }
    }

    private void supprimerFichier() {
        String fichierSelectionne = listeFichiers.getSelectionModel().getSelectedItem();
        if (fichierSelectionne != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Supprimer le fichier audio");
            confirmation.setContentText("Êtes-vous sûr de vouloir supprimer ce fichier ?");
            
            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    String cheminComplet = "audio/generated/" + fichierSelectionne;
                    File fichier = new File(cheminComplet);
                    
                    if (fichier.delete()) {
                        rafraichirHistorique();
                        afficherInfo("Succès", "Fichier supprimé avec succès.");
                    } else {
                        afficherErreur("Erreur", "Impossible de supprimer le fichier.");
                    }
                } catch (Exception e) {
                    afficherErreur("Erreur", "Erreur lors de la suppression: " + e.getMessage());
                }
            }
        }
    }

    private void ouvrirDossierAudio() {
        try {
            File dossier = new File("audio/generated/");
            if (dossier.exists()) {
                java.awt.Desktop.getDesktop().open(dossier);
            } else {
                afficherErreur("Erreur", "Le dossier audio n'existe pas encore.");
            }
        } catch (Exception e) {
            afficherErreur("Erreur", "Impossible d'ouvrir le dossier: " + e.getMessage());
        }
    }

    private void rafraichirHistorique() {
        try {
            File dossier = new File("audio/generated/");
            if (dossier.exists() && dossier.isDirectory()) {
                File[] fichiers = dossier.listFiles((dir, name) -> 
                    name.toLowerCase().endsWith(".mp3") || 
                    name.toLowerCase().endsWith(".wav") || 
                    name.toLowerCase().endsWith(".ogg"));
                
                if (fichiers != null) {
                    String[] nomsFichiers = new String[fichiers.length];
                    for (int i = 0; i < fichiers.length; i++) {
                        nomsFichiers[i] = fichiers[i].getName();
                    }
                    
                    Platform.runLater(() -> {
                        listeFichiers.setItems(FXCollections.observableArrayList(nomsFichiers));
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du rafraîchissement de l'historique: " + e.getMessage());
        }
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
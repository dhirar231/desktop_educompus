package com.educompus.controller.back;

import com.educompus.service.HeyGenVideoService;
import com.educompus.service.GoogleTTSService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur pour l'interface de génération de vidéos HeyGen.
 */
public class BackHeyGenController implements Initializable {

    @FXML private TextArea scriptTextArea;
    @FXML private ComboBox<String> avatarComboBox;
    @FXML private ComboBox<String> voixComboBox;
    @FXML private ComboBox<String> qualiteComboBox;
    @FXML private ComboBox<String> ratioComboBox;
    @FXML private CheckBox sousTitresCheckBox;
    @FXML private Slider vitesseSlider;
    @FXML private Label vitesseLabel;
    @FXML private Button genererButton;
    @FXML private Button testerButton;
    @FXML private Button genererAudioButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private WebView videoWebView;
    @FXML private TextArea resultatTextArea;
    @FXML private Button jouerAudioButton;

    private MediaPlayer audioPlayer;
    private String dernierFichierAudio;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initialiserComposants();
        configurerEvenements();
    }

    private void initialiserComposants() {
        // Initialiser les avatars
        String[] avatars = HeyGenVideoService.getAvatarsDisponibles();
        avatarComboBox.getItems().addAll(avatars);
        avatarComboBox.setValue(avatars[0]); // Professeure Claire par défaut

        // Initialiser les voix françaises
        voixComboBox.getItems().addAll(
            "fr-FR-DeniseNeural",
            "fr-FR-HenriNeural", 
            "fr-FR-AlainNeural",
            "fr-FR-BrigitteNeural",
            "fr-FR-CelesteNeural",
            "fr-FR-ClaudeNeural",
            "fr-FR-CoralieNeural"
        );
        voixComboBox.setValue("fr-FR-DeniseNeural");

        // Initialiser les qualités
        qualiteComboBox.getItems().addAll("low", "medium", "high");
        qualiteComboBox.setValue("high");

        // Initialiser les ratios
        ratioComboBox.getItems().addAll("16:9", "9:16", "1:1");
        ratioComboBox.setValue("16:9");

        // Initialiser les autres contrôles
        sousTitresCheckBox.setSelected(true);
        vitesseSlider.setValue(0.9);
        vitesseLabel.setText("0.9x");

        // Script d'exemple
        scriptTextArea.setText("""
            Bonjour et bienvenue dans cette vidéo éducative !
            
            Aujourd'hui, nous allons explorer ensemble un concept fascinant qui vous aidera dans votre apprentissage.
            
            Commençons par comprendre les bases de ce sujet important.
            
            Prenons un exemple concret pour illustrer ces concepts.
            
            Pour résumer, nous avons vu aujourd'hui les points essentiels.
            
            Merci de votre attention et à bientôt pour de nouvelles découvertes !
            """);

        // État initial
        progressBar.setVisible(false);
        statusLabel.setText("Prêt à générer une vidéo HeyGen");
        jouerAudioButton.setDisable(true);
    }

    private void configurerEvenements() {
        // Mise à jour du label de vitesse
        vitesseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double vitesse = Math.round(newVal.doubleValue() * 10.0) / 10.0;
            vitesseLabel.setText(vitesse + "x");
        });

        // Boutons
        genererButton.setOnAction(e -> genererVideo());
        testerButton.setOnAction(e -> testerConfiguration());
        genererAudioButton.setOnAction(e -> genererAudio());
        jouerAudioButton.setOnAction(e -> jouerAudio());
    }

    @FXML
    private void genererVideo() {
        String script = scriptTextArea.getText().trim();
        if (script.isEmpty()) {
            afficherErreur("Veuillez saisir un script pour la vidéo.");
            return;
        }

        if (script.length() > 10000) {
            afficherErreur("Le script est trop long (maximum 10000 caractères).");
            return;
        }

        // Désactiver l'interface pendant la génération
        desactiverInterface(true);
        progressBar.setVisible(true);
        statusLabel.setText("Génération de la vidéo en cours...");
        resultatTextArea.clear();

        // Créer les paramètres HeyGen
        HeyGenVideoService.ParametresHeyGen parametres = creerParametres();

        // Générer la vidéo de manière asynchrone
        Task<HeyGenVideoService.ResultatHeyGen> task = new Task<>() {
            @Override
            protected HeyGenVideoService.ResultatHeyGen call() throws Exception {
                return HeyGenVideoService.genererVideo(script, parametres);
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    HeyGenVideoService.ResultatHeyGen resultat = getValue();
                    gererResultatVideo(resultat);
                    desactiverInterface(false);
                    progressBar.setVisible(false);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    afficherErreur("Erreur lors de la génération: " + exception.getMessage());
                    desactiverInterface(false);
                    progressBar.setVisible(false);
                });
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void testerConfiguration() {
        desactiverInterface(true);
        progressBar.setVisible(true);
        statusLabel.setText("Test de la configuration HeyGen...");

        Task<HeyGenVideoService.ResultatHeyGen> task = new Task<>() {
            @Override
            protected HeyGenVideoService.ResultatHeyGen call() throws Exception {
                return HeyGenVideoService.testerConfiguration();
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    HeyGenVideoService.ResultatHeyGen resultat = getValue();
                    if (resultat.isSucces()) {
                        afficherSucces("✅ Configuration HeyGen fonctionnelle !");
                        gererResultatVideo(resultat);
                    } else {
                        afficherErreur("❌ Test échoué: " + resultat.getMessageErreur());
                    }
                    desactiverInterface(false);
                    progressBar.setVisible(false);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    afficherErreur("❌ Erreur lors du test: " + getException().getMessage());
                    desactiverInterface(false);
                    progressBar.setVisible(false);
                });
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void genererAudio() {
        String script = scriptTextArea.getText().trim();
        if (script.isEmpty()) {
            afficherErreur("Veuillez saisir un script pour l'audio.");
            return;
        }

        desactiverInterface(true);
        statusLabel.setText("Génération de l'audio TTS...");

        GoogleTTSService.ParametresTTS parametresTTS = new GoogleTTSService.ParametresTTS();
        parametresTTS.setVoix(GoogleTTSService.VoixFrancaise.FEMALE_A);
        
        CompletableFuture<GoogleTTSService.ResultatTTS> future = 
            GoogleTTSService.convertirTexteAsync(script, parametresTTS);

        future.thenAccept(resultat -> {
            Platform.runLater(() -> {
                if (resultat.isSucces()) {
                    dernierFichierAudio = resultat.getCheminFichier();
                    jouerAudioButton.setDisable(false);
                    afficherSucces("✅ Audio généré: " + resultat.getCheminFichier());
                } else {
                    afficherErreur("❌ Erreur TTS: " + resultat.getMessageErreur());
                }
                desactiverInterface(false);
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                afficherErreur("❌ Erreur TTS: " + throwable.getMessage());
                desactiverInterface(false);
            });
            return null;
        });
    }

    @FXML
    private void jouerAudio() {
        if (dernierFichierAudio == null) {
            afficherErreur("Aucun fichier audio à jouer.");
            return;
        }

        try {
            // Arrêter l'audio précédent s'il existe
            if (audioPlayer != null) {
                audioPlayer.stop();
                audioPlayer.dispose();
            }

            // Jouer le nouveau fichier
            Media media = new Media("file:///" + dernierFichierAudio.replace("\\", "/"));
            audioPlayer = new MediaPlayer(media);
            audioPlayer.setOnReady(() -> {
                statusLabel.setText("🔊 Lecture audio: " + audioPlayer.getTotalDuration().toSeconds() + "s");
                audioPlayer.play();
            });
            audioPlayer.setOnEndOfMedia(() -> {
                statusLabel.setText("✅ Lecture terminée");
            });
            audioPlayer.setOnError(() -> {
                afficherErreur("Erreur lors de la lecture audio");
            });

        } catch (Exception e) {
            afficherErreur("Impossible de jouer l'audio: " + e.getMessage());
        }
    }

    private HeyGenVideoService.ParametresHeyGen creerParametres() {
        HeyGenVideoService.ParametresHeyGen parametres = new HeyGenVideoService.ParametresHeyGen();
        
        // Trouver l'avatar sélectionné
        String avatarDescription = avatarComboBox.getValue();
        for (HeyGenVideoService.AvatarEducatif avatar : HeyGenVideoService.AvatarEducatif.values()) {
            if (avatar.getDescription().equals(avatarDescription)) {
                parametres.setAvatar(avatar);
                break;
            }
        }
        
        parametres.setVoix(voixComboBox.getValue());
        parametres.setQualite(qualiteComboBox.getValue());
        parametres.setRatio(ratioComboBox.getValue());
        parametres.setSousTitres(sousTitresCheckBox.isSelected());
        parametres.setVitesseParole(vitesseSlider.getValue());
        
        return parametres;
    }

    private void gererResultatVideo(HeyGenVideoService.ResultatHeyGen resultat) {
        if (resultat.isSucces()) {
            afficherSucces("✅ Vidéo générée avec succès !");
            
            // Afficher les détails
            StringBuilder details = new StringBuilder();
            details.append("🎬 VIDÉO GÉNÉRÉE AVEC SUCCÈS\n\n");
            details.append("📹 URL: ").append(resultat.getUrlVideo()).append("\n");
            details.append("⏱️ Durée: ").append(resultat.getDureeFormatee()).append("\n");
            details.append("🆔 ID: ").append(resultat.getVideoId()).append("\n");
            if (resultat.getUrlThumbnail() != null) {
                details.append("🖼️ Miniature: ").append(resultat.getUrlThumbnail()).append("\n");
            }
            details.append("\n📋 PARAMÈTRES UTILISÉS:\n");
            details.append(creerParametres().toString());
            
            resultatTextArea.setText(details.toString());

            // Charger la vidéo dans le WebView si possible
            if (resultat.getUrlVideo() != null && !resultat.getUrlVideo().contains("example.com")) {
                chargerVideoWebView(resultat.getUrlVideo());
            }
            
        } else if (resultat.estEnCours()) {
            statusLabel.setText("⏳ Génération en cours: " + resultat.getStatut());
            resultatTextArea.setText("🔄 Génération en cours...\n\nStatut: " + resultat.getStatut());
            
        } else {
            afficherErreur("❌ Erreur: " + resultat.getMessageErreur());
            resultatTextArea.setText("❌ ERREUR\n\n" + resultat.getMessageErreur());
        }
    }

    private void chargerVideoWebView(String urlVideo) {
        try {
            // Créer un lecteur vidéo HTML simple
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { margin: 0; padding: 10px; background: #f0f0f0; }
                        video { width: 100%%; height: auto; border-radius: 8px; }
                        .info { font-family: Arial; font-size: 12px; color: #666; margin-top: 10px; }
                    </style>
                </head>
                <body>
                    <video controls>
                        <source src="%s" type="video/mp4">
                        Votre navigateur ne supporte pas la lecture vidéo.
                    </video>
                    <div class="info">
                        📹 Vidéo générée par HeyGen<br>
                        🔗 <a href="%s" target="_blank">Ouvrir dans le navigateur</a>
                    </div>
                </body>
                </html>
                """, urlVideo, urlVideo);
            
            videoWebView.getEngine().loadContent(htmlContent);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la vidéo: " + e.getMessage());
        }
    }

    private void desactiverInterface(boolean desactiver) {
        genererButton.setDisable(desactiver);
        testerButton.setDisable(desactiver);
        genererAudioButton.setDisable(desactiver);
        scriptTextArea.setDisable(desactiver);
        avatarComboBox.setDisable(desactiver);
        voixComboBox.setDisable(desactiver);
        qualiteComboBox.setDisable(desactiver);
        ratioComboBox.setDisable(desactiver);
        sousTitresCheckBox.setDisable(desactiver);
        vitesseSlider.setDisable(desactiver);
    }

    private void afficherSucces(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: green;");
    }

    private void afficherErreur(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
        
        // Afficher aussi dans une alerte
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Erreur lors de la génération");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Nettoie les ressources lors de la fermeture.
     */
    public void cleanup() {
        if (audioPlayer != null) {
            audioPlayer.stop();
            audioPlayer.dispose();
        }
    }
}
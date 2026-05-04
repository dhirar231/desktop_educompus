package com.educompus.controller.back;

import com.educompus.service.VideoCache;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur pour la gestion du cache des vidéos IA.
 */
public class BackVideoCacheController implements Initializable {

    @FXML private TextArea contenuTextArea;
    @FXML private ComboBox<String> avatarComboBox;
    @FXML private ComboBox<String> voixComboBox;
    @FXML private ComboBox<String> qualiteComboBox;
    @FXML private CheckBox forceRegenerationCheckBox;
    @FXML private Button genererButton;
    @FXML private Button viderCacheButton;
    @FXML private Button statistiquesButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private TextArea resultatTextArea;
    @FXML private TableView<CacheEntryDisplay> cacheTableView;
    @FXML private TableColumn<CacheEntryDisplay, String> hashColumn;
    @FXML private TableColumn<CacheEntryDisplay, String> dateColumn;
    @FXML private TableColumn<CacheEntryDisplay, String> tailleColumn;
    @FXML private TableColumn<CacheEntryDisplay, String> statutColumn;

    private VideoCache videoCache;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        videoCache = new VideoCache();
        initialiserComposants();
        configurerEvenements();
        configurerTableau();
    }

    private void initialiserComposants() {
        // Avatars
        avatarComboBox.getItems().addAll(
            "MADISON_1",
            "MADISON_2", 
            "MADISON_3",
            "MADISON_4",
            "MADISON_5",
            "MADISON_6"
        );
        avatarComboBox.setValue("MADISON_1");

        // Voix
        voixComboBox.getItems().addAll(
            "fr-FR-DeniseNeural",
            "fr-FR-HenriNeural", 
            "fr-FR-AlainNeural",
            "fr-FR-BrigitteNeural",
            "fr-FR-CelesteNeural"
        );
        voixComboBox.setValue("fr-FR-DeniseNeural");

        // Qualité
        qualiteComboBox.getItems().addAll("low", "medium", "high");
        qualiteComboBox.setValue("medium");

        // Contenu d'exemple
        contenuTextArea.setText("""
            Les algorithmes de tri sont des méthodes fondamentales en informatique.
            
            Ils permettent d'organiser les données selon un ordre défini.
            
            Nous étudierons le tri à bulles, le tri par insertion et le tri rapide.
            """);

        // État initial
        progressBar.setVisible(false);
        statusLabel.setText("Prêt à utiliser le cache intelligent");
        forceRegenerationCheckBox.setSelected(false);
    }

    private void configurerEvenements() {
        genererButton.setOnAction(e -> genererAvecCache());
        viderCacheButton.setOnAction(e -> viderCache());
        statistiquesButton.setOnAction(e -> afficherStatistiques());
    }

    private void configurerTableau() {
        hashColumn.setCellValueFactory(new PropertyValueFactory<>("hash"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        tailleColumn.setCellValueFactory(new PropertyValueFactory<>("taille"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
    }

    @FXML
    private void genererAvecCache() {
        String contenu = contenuTextArea.getText().trim();
        if (contenu.isEmpty()) {
            afficherErreur("Veuillez saisir du contenu à traiter.");
            return;
        }

        desactiverInterface(true);
        progressBar.setVisible(true);
        statusLabel.setText("Vérification du cache...");
        resultatTextArea.clear();

        VideoCache.ParametresCache parametres = new VideoCache.ParametresCache(
            contenu,
            avatarComboBox.getValue(),
            voixComboBox.getValue(),
            qualiteComboBox.getValue(),
            forceRegenerationCheckBox.isSelected()
        );

        Task<VideoCache.CacheEntry> task = new Task<>() {
            @Override
            protected VideoCache.CacheEntry call() throws Exception {
                updateMessage("Recherche dans le cache...");
                
                CompletableFuture<VideoCache.CacheEntry> future = videoCache.obtenirVideo(parametres);
                
                // Simuler le progrès
                for (int i = 0; i <= 100; i += 10) {
                    if (isCancelled()) break;
                    updateProgress(i, 100);
                    Thread.sleep(200);
                }
                
                return future.get();
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    VideoCache.CacheEntry entree = getValue();
                    gererResultatCache(entree);
                    desactiverInterface(false);
                    progressBar.setVisible(false);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    afficherErreur("Erreur: " + exception.getMessage());
                    desactiverInterface(false);
                    progressBar.setVisible(false);
                });
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void viderCache() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Vider le cache");
        confirmation.setHeaderText("Êtes-vous sûr ?");
        confirmation.setContentText("Cette action supprimera toutes les vidéos en cache.");
        
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            videoCache.nettoyerCache();
            afficherSucces("✅ Cache vidé avec succès");
            actualiserTableau();
        }
    }

    @FXML
    private void afficherStatistiques() {
        String stats = videoCache.getStatistiques();
        resultatTextArea.setText(stats);
        statusLabel.setText("Statistiques du cache affichées");
    }

    private void gererResultatCache(VideoCache.CacheEntry entree) {
        if (entree != null) {
            StringBuilder resultat = new StringBuilder();
            resultat.append("🎬 VIDÉO OBTENUE DEPUIS LE CACHE\n\n");
            resultat.append("🔑 Hash: ").append(entree.getHash()).append("\n");
            resultat.append("📅 Date création: ").append(entree.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
            resultat.append("📹 URL vidéo: ").append(entree.getUrlVideo()).append("\n");
            
            if (entree.getCheminAudio() != null) {
                resultat.append("🔊 Audio: ").append(entree.getCheminAudio()).append("\n");
            }
            
            resultat.append("📊 Taille: ").append(entree.getTailleFormatee()).append("\n");
            resultat.append("⏰ Expiré: ").append(entree.isExpire() ? "Oui" : "Non").append("\n");
            resultat.append("📁 Fichier existe: ").append(entree.fichierExiste() ? "Oui" : "Non").append("\n\n");
            
            if (entree.getMetadata() != null) {
                resultat.append("📝 MÉTADONNÉES:\n");
                resultat.append("Titre: ").append(entree.getMetadata().getTitre()).append("\n");
                resultat.append("Description: ").append(entree.getMetadata().getDescription()).append("\n");
                if (entree.getMetadata().getAiScript() != null) {
                    resultat.append("Script (extrait): ").append(
                        entree.getMetadata().getAiScript().substring(0, Math.min(200, entree.getMetadata().getAiScript().length()))
                    ).append("...\n");
                }
            }
            
            resultatTextArea.setText(resultat.toString());
            afficherSucces("✅ Vidéo obtenue depuis le cache !");
            
            // Actualiser le tableau
            actualiserTableau();
            
        } else {
            afficherErreur("❌ Impossible d'obtenir la vidéo");
        }
    }

    private void actualiserTableau() {
        // Pour l'instant, le tableau reste vide car VideoCache ne expose pas la liste
        // Dans une implémentation complète, vous ajouteriez une méthode pour lister les entrées
        cacheTableView.getItems().clear();
    }

    private void desactiverInterface(boolean desactiver) {
        genererButton.setDisable(desactiver);
        viderCacheButton.setDisable(desactiver);
        statistiquesButton.setDisable(desactiver);
        contenuTextArea.setDisable(desactiver);
        avatarComboBox.setDisable(desactiver);
        voixComboBox.setDisable(desactiver);
        qualiteComboBox.setDisable(desactiver);
        forceRegenerationCheckBox.setDisable(desactiver);
    }

    private void afficherSucces(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: green;");
    }

    private void afficherErreur(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Erreur de cache");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Classe pour l'affichage dans le tableau.
     */
    public static class CacheEntryDisplay {
        private final String hash;
        private final String date;
        private final String taille;
        private final String statut;

        public CacheEntryDisplay(String hash, String date, String taille, String statut) {
            this.hash = hash;
            this.date = date;
            this.taille = taille;
            this.statut = statut;
        }

        public String getHash() { return hash; }
        public String getDate() { return date; }
        public String getTaille() { return taille; }
        public String getStatut() { return statut; }
    }
}
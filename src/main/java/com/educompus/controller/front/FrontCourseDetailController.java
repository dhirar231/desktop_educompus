package com.educompus.controller.front;

import com.educompus.app.AppState;
import com.educompus.model.Chapitre;
import com.educompus.model.Cours;
import com.educompus.model.Td;
import com.educompus.model.VideoExplicative;
import com.educompus.repository.ChapitreProgressRepository;
import com.educompus.repository.CourseManagementRepository;
import com.educompus.service.CourseFavoriteService;
import com.educompus.service.MyMemoryTranslationService;
import com.educompus.nav.Navigator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Set;

public final class FrontCourseDetailController {

    @FXML private Label coursTitle;
    @FXML private Label coursDescription;
    @FXML private Label niveauChip;
    @FXML private Label domaineChip;
    @FXML private Label formateurLabel;
    @FXML private Label dureeLabel;
    @FXML private Label chapitresCountLabel;
    @FXML private Label chapitresTotal;
    @FXML private Label dateLabel;
    @FXML private Label breadcrumb;
    @FXML private Label niveauInfoLabel;
    @FXML private VBox chapitresBox;
    @FXML private VBox driveCard;
    @FXML private Button btnOpenDrive;
    @FXML private ComboBox<MyMemoryTranslationService.Language> langCombo;
    @FXML private Button btnTranslate;
    @FXML private ProgressIndicator translateSpinner;
    @FXML private Label translateStatusLabel;

    // Labels statiques traduisibles
    @FXML private Label lblAbout;
    @FXML private Label lblChapitres;
    @FXML private Label lblInfos;
    @FXML private Label lblFormateur;
    @FXML private Label lblDuree;
    @FXML private Label lblChapitresInfo;
    @FXML private Label lblNiveau;
    @FXML private Label lblDate;
    @FXML private Label lblProgression;

    // Widget mini traducteur
    @FXML private javafx.scene.layout.StackPane miniTranslatorWidget;
    @FXML private ComboBox<MyMemoryTranslationService.Language> miniSourceLang;
    @FXML private ComboBox<MyMemoryTranslationService.Language> miniTargetLang;
    @FXML private javafx.scene.control.TextField miniSourceText;
    @FXML private Label miniResultText;
    @FXML private Button btnMiniTranslate;
    @FXML private Button btnMiniSwap;
    @FXML private Button btnMiniCopy;
    @FXML private Button btnCloseMiniWidget;
    @FXML private ProgressIndicator miniSpinner;
    @FXML private Label miniStatus;

    // Composants pour le lecteur vidéo intégré
    @FXML private StackPane videoModal;
    @FXML private javafx.scene.media.MediaView mediaView;
    @FXML private Label videoTitle;
    @FXML private Button playPauseBtn;
    @FXML private Button closeVideoBtn;
    @FXML private Button fullscreenBtn;
    @FXML private javafx.scene.control.Slider volumeSlider;
    @FXML private javafx.scene.control.Slider progressSlider;
    @FXML private Label timeLabel;

    // Variables pour la gestion du lecteur vidéo
    private MediaPlayer currentMediaPlayer;
    private boolean isPlaying = false;

    private final CourseManagementRepository repo = new CourseManagementRepository();
    private final ChapitreProgressRepository progressRepo = new ChapitreProgressRepository();
    private Cours cours;
    private Set<Integer> completedIds;
    private CourseFavoriteService favoriteService;
    private int favoriteStudentId;

    // Langue source détectée (toujours "fr" pour ce projet)
    private static final String SOURCE_LANG = "fr";
    // Langue actuellement affichée
    private MyMemoryTranslationService.Language currentLang = MyMemoryTranslationService.Language.FR;

    public void setCours(Cours c) {
        this.cours = c;
        populate();
    }

    public void setFavoriteContext(CourseFavoriteService favoriteService, int studentId) {
        this.favoriteService = favoriteService;
        this.favoriteStudentId = studentId;
    }

    @FXML
    private void initialize() {
        // DIAGNOSTIC: Vérifier l'état des composants à l'initialisation
        com.educompus.debug.VideoPlayerDiagnostic.afficherRapportDiagnostic(this, "INITIALIZE");
        
        // Initialiser le ComboBox des langues
        if (langCombo != null) {
            langCombo.setItems(FXCollections.observableArrayList(MyMemoryTranslationService.Language.values()));
            langCombo.setValue(MyMemoryTranslationService.Language.FR);
        }
        if (translateSpinner != null) {
            translateSpinner.setVisible(false);
            translateSpinner.setManaged(false);
        }
        if (translateStatusLabel != null) {
            translateStatusLabel.setVisible(false);
            translateStatusLabel.setManaged(false);
        }

        // Initialiser le widget mini traducteur
        if (miniSourceLang != null) {
            miniSourceLang.setItems(FXCollections.observableArrayList(MyMemoryTranslationService.Language.values()));
            miniSourceLang.setValue(MyMemoryTranslationService.Language.FR);
        }
        if (miniTargetLang != null) {
            miniTargetLang.setItems(FXCollections.observableArrayList(MyMemoryTranslationService.Language.values()));
            miniTargetLang.setValue(MyMemoryTranslationService.Language.EN);
        }
        if (miniSpinner != null) {
            miniSpinner.setVisible(false);
            miniSpinner.setManaged(false);
        }
        if (miniStatus != null) {
            miniStatus.setVisible(false);
            miniStatus.setManaged(false);
        }

        // Initialiser le modal vidéo
        if (videoModal != null) {
            videoModal.setVisible(false);
            videoModal.setManaged(false);
            
            // Ajouter la gestion des événements clavier pour fermer avec Échap
            videoModal.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    onCloseVideo();
                }
            });
            
            // Permettre au modal de recevoir le focus pour les événements clavier
            videoModal.setFocusTraversable(true);
        }
    }

    @FXML
    private void onTranslate() {
        if (cours == null || langCombo == null) return;
        MyMemoryTranslationService.Language targetLang = langCombo.getValue();
        if (targetLang == null) return;

        // Si même langue que la source, recharger l'original
        if (targetLang == MyMemoryTranslationService.Language.FR) {
            currentLang = MyMemoryTranslationService.Language.FR;
            populate();
            restoreStaticLabels();
            return;
        }

        setTranslating(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                String toLang = targetLang.code;

                // Traduire titre et description du cours
                String newTitre = MyMemoryTranslationService.translate(cours.getTitre(), SOURCE_LANG, toLang);
                String newDesc  = MyMemoryTranslationService.translate(cours.getDescription(), SOURCE_LANG, toLang);

                // Traduire les labels statiques de la page
                String tAbout        = MyMemoryTranslationService.translate("À propos de ce cours", SOURCE_LANG, toLang);
                String tChapitres    = MyMemoryTranslationService.translate("Chapitres", SOURCE_LANG, toLang);
                String tInfos        = MyMemoryTranslationService.translate("Informations", SOURCE_LANG, toLang);
                String tFormateur    = MyMemoryTranslationService.translate("Formateur", SOURCE_LANG, toLang);
                String tDuree        = MyMemoryTranslationService.translate("Durée totale", SOURCE_LANG, toLang);
                String tNiveau       = MyMemoryTranslationService.translate("Niveau", SOURCE_LANG, toLang);
                String tDate         = MyMemoryTranslationService.translate("Date de création", SOURCE_LANG, toLang);
                String tProgression  = MyMemoryTranslationService.translate("Ma progression", SOURCE_LANG, toLang);
                String tMarquerLu    = MyMemoryTranslationService.translate("Marquer comme lu", SOURCE_LANG, toLang);
                String tLu           = MyMemoryTranslationService.translate("Lu", SOURCE_LANG, toLang);
                String tTDs          = MyMemoryTranslationService.translate("Travaux Dirigés", SOURCE_LANG, toLang);
                String tVideos       = MyMemoryTranslationService.translate("Vidéos explicatives", SOURCE_LANG, toLang);
                String tTelecharger  = MyMemoryTranslationService.translate("Télécharger", SOURCE_LANG, toLang);
                String tRegarder     = MyMemoryTranslationService.translate("Regarder", SOURCE_LANG, toLang);
                String tDescription  = MyMemoryTranslationService.translate("Description", SOURCE_LANG, toLang);
                String tRetour       = MyMemoryTranslationService.translate("Retour aux cours", SOURCE_LANG, toLang);

                // Charger et traduire les chapitres, TD et vidéos
                List<Chapitre> chapitres = repo.listChapitresByCoursId(cours.getId());
                List<Td> tds = repo.listTdsByCoursId(cours.getId());
                List<VideoExplicative> videos = repo.listVideosByCoursId(cours.getId());

                // Traduire tous les TD
                List<Td> translatedTds = new java.util.ArrayList<>();
                for (Td td : tds) {
                    String tdTitre = MyMemoryTranslationService.translate(td.getTitre(), SOURCE_LANG, toLang);
                    String tdDesc  = MyMemoryTranslationService.translate(td.getDescription(), SOURCE_LANG, toLang);
                    translatedTds.add(cloneTd(td, tdTitre, tdDesc));
                }

                // Traduire toutes les vidéos
                List<VideoExplicative> translatedVideos = new java.util.ArrayList<>();
                for (VideoExplicative video : videos) {
                    String vidTitre = MyMemoryTranslationService.translate(video.getTitre(), SOURCE_LANG, toLang);
                    String vidDesc  = MyMemoryTranslationService.translate(video.getDescription(), SOURCE_LANG, toLang);
                    translatedVideos.add(cloneVideo(video, vidTitre, vidDesc));
                }

                // Stocker les traductions pour les labels dynamiques des cartes
                java.util.Map<String, String> uiT = new java.util.HashMap<>();
                uiT.put("marquerLu", tMarquerLu);
                uiT.put("lu", tLu);
                uiT.put("tds", tTDs);
                uiT.put("videos", tVideos);
                uiT.put("telecharger", tTelecharger);
                uiT.put("regarder", tRegarder);
                uiT.put("description", tDescription);

                Platform.runLater(() -> {
                    currentLang = targetLang;

                    // ── Labels statiques ──
                    if (lblAbout != null)       lblAbout.setText(tAbout);
                    if (lblChapitres != null)   lblChapitres.setText(tChapitres);
                    if (lblInfos != null)       lblInfos.setText(tInfos);
                    if (lblFormateur != null)   lblFormateur.setText(tFormateur);
                    if (lblDuree != null)       lblDuree.setText(tDuree);
                    if (lblChapitresInfo != null) lblChapitresInfo.setText(tChapitres);
                    if (lblNiveau != null)      lblNiveau.setText(tNiveau);
                    if (lblDate != null)        lblDate.setText(tDate);
                    if (lblProgression != null) lblProgression.setText(tProgression);

                    // Mettre à jour titre et description du cours
                    coursTitle.setText(newTitre);
                    coursDescription.setText(newDesc);
                    if (breadcrumb != null) breadcrumb.setText(newTitre);

                    // Reconstruire les cartes chapitres avec traduction complète
                    chapitresBox.getChildren().clear();
                    if (chapitres.isEmpty()) {
                        Label empty = new Label(MyMemoryTranslationService.translate("Aucun chapitre disponible.", SOURCE_LANG, toLang));
                        empty.getStyleClass().add("page-subtitle");
                        chapitresBox.getChildren().add(empty);
                    } else {
                        for (Chapitre ch : chapitres) {
                            // Traduire titre et description du chapitre
                            String chTitre = MyMemoryTranslationService.translate(ch.getTitre(), SOURCE_LANG, toLang);
                            String chDesc  = MyMemoryTranslationService.translate(ch.getDescription(), SOURCE_LANG, toLang);
                            Chapitre translated = cloneChapitre(ch, chTitre, chDesc);

                            // Filtrer les TD et vidéos traduits pour ce chapitre
                            List<Td> chTds = translatedTds.stream()
                                    .filter(t -> t.getChapitreId() == ch.getId()).toList();
                            List<VideoExplicative> chVideos = translatedVideos.stream()
                                    .filter(v -> v.getChapitreId() == ch.getId()).toList();

                            chapitresBox.getChildren().add(buildChapitreCard(translated, chTds, chVideos, uiT));
                        }
                    }
                    setTranslating(false);
                    showStatus("✓ Traduit en " + targetLang.label);
                });
                return null;
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    setTranslating(false);
                    showStatus("⚠ Erreur de traduction");
                });
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /** Restaure les labels statiques en français. */
    private void restoreStaticLabels() {
        if (lblAbout != null)         lblAbout.setText("À propos de ce cours");
        if (lblChapitres != null)     lblChapitres.setText("Chapitres");
        if (lblInfos != null)         lblInfos.setText("Informations");
        if (lblFormateur != null)     lblFormateur.setText("Formateur");
        if (lblDuree != null)         lblDuree.setText("Durée totale");
        if (lblChapitresInfo != null) lblChapitresInfo.setText("Chapitres");
        if (lblNiveau != null)        lblNiveau.setText("Niveau");
        if (lblDate != null)          lblDate.setText("Date de création");
        if (lblProgression != null)   lblProgression.setText("Ma progression");
    }

    private void setTranslating(boolean loading) {
        if (translateSpinner != null) {
            translateSpinner.setVisible(loading);
            translateSpinner.setManaged(loading);
        }
        if (btnTranslate != null) btnTranslate.setDisable(loading);
        if (langCombo != null) langCombo.setDisable(loading);
    }

    private void showStatus(String msg) {
        if (translateStatusLabel == null) return;
        translateStatusLabel.setText(msg);
        translateStatusLabel.setVisible(true);
        translateStatusLabel.setManaged(true);
        // Masquer après 3 secondes
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
        pause.setOnFinished(e -> {
            translateStatusLabel.setVisible(false);
            translateStatusLabel.setManaged(false);
        });
        pause.play();
    }

    /** Clone un chapitre avec un titre et description traduits (sans modifier l'original). */
    private static Chapitre cloneChapitre(Chapitre original, String newTitre, String newDesc) {
        Chapitre c = new Chapitre();
        c.setId(original.getId());
        c.setOrdre(original.getOrdre());
        c.setTitre(newTitre);
        c.setDescription(newDesc);
        c.setFichierC(original.getFichierC());
        c.setCoursId(original.getCoursId());
        return c;
    }

    /** Clone un TD avec titre et description traduits. */
    private static Td cloneTd(Td original, String newTitre, String newDesc) {
        Td td = new Td();
        td.setId(original.getId());
        td.setTitre(newTitre);
        td.setDescription(newDesc);
        td.setFichier(original.getFichier());
        td.setChapitreId(original.getChapitreId());
        td.setCoursId(original.getCoursId());
        td.setNiveau(original.getNiveau());
        td.setDomaine(original.getDomaine());
        return td;
    }

    /** Clone une vidéo avec titre et description traduits. */
    private static VideoExplicative cloneVideo(VideoExplicative original, String newTitre, String newDesc) {
        VideoExplicative video = new VideoExplicative();
        video.setId(original.getId());
        video.setTitre(newTitre);
        video.setDescription(newDesc);
        video.setUrlVideo(original.getUrlVideo());
        video.setChapitreId(original.getChapitreId());
        video.setCoursId(original.getCoursId());
        video.setNiveau(original.getNiveau());
        video.setDomaine(original.getDomaine());
        return video;
    }

    private void populate() {
        if (cours == null) return;

        coursTitle.setText(safe(cours.getTitre()));
        coursDescription.setText(safe(cours.getDescription()));
        niveauChip.setText(safe(cours.getNiveau()).isBlank() ? "Tous niveaux" : safe(cours.getNiveau()));
        domaineChip.setText(safe(cours.getDomaine()).isBlank() ? "Général" : safe(cours.getDomaine()));
        formateurLabel.setText(safe(cours.getNomFormateur()).isBlank() ? "Non renseigné" : safe(cours.getNomFormateur()));
        dureeLabel.setText(cours.getDureeTotaleHeures() + "h de contenu");
        breadcrumb.setText(safe(cours.getTitre()));
        if (niveauInfoLabel != null) niveauInfoLabel.setText(safe(cours.getNiveau()).isBlank() ? "Tous niveaux" : safe(cours.getNiveau()));

        if (cours.getDateCreation() != null) {
            dateLabel.setText("Créé le " + cours.getDateCreation().toString().substring(0, 10));
        }

        // Résoudre l'URL Drive : priorité à driveLink, fallback sur driveFolderId
        String driveUrl = null;
        String driveLink = cours.getDriveLink();
        String driveFolderId = cours.getDriveFolderId();
        if (driveLink != null && !driveLink.isBlank()) {
            driveUrl = driveLink;
        } else if (driveFolderId != null && !driveFolderId.isBlank() && !driveFolderId.equals("EN_ATTENTE")) {
            driveUrl = "https://drive.google.com/drive/folders/" + driveFolderId;
        }

        if (driveUrl != null) {
            final String finalDriveUrl = driveUrl;
            if (driveCard != null) {
                driveCard.setVisible(true);
                driveCard.setManaged(true);
            }
            if (btnOpenDrive != null) {
                btnOpenDrive.setOnAction(e -> openUrl(finalDriveUrl));
            }
        } else {
            if (driveCard != null) {
                driveCard.setVisible(false);
                driveCard.setManaged(false);
            }
        }

        // Charger la progression de l'étudiant
        completedIds = progressRepo.getCompletedChapitres(AppState.getUserId(), cours.getId());

        List<Chapitre> chapitres = repo.listChapitresByCoursId(cours.getId());
        List<Td> tds = repo.listTdsByCoursId(cours.getId());
        List<VideoExplicative> videos = repo.listVideosByCoursId(cours.getId());

        chapitresCountLabel.setText(chapitres.size() + " chapitre" + (chapitres.size() > 1 ? "s" : ""));
        chapitresTotal.setText(String.valueOf(chapitres.size()));

        chapitresBox.getChildren().clear();
        if (chapitres.isEmpty()) {
            Label empty = new Label("Aucun chapitre disponible.");
            empty.getStyleClass().add("page-subtitle");
            chapitresBox.getChildren().add(empty);
            return;
        }

        for (Chapitre ch : chapitres) {
            List<Td> chTds = tds.stream().filter(t -> t.getChapitreId() == ch.getId()).toList();
            List<VideoExplicative> chVideos = videos.stream().filter(v -> v.getChapitreId() == ch.getId()).toList();
            chapitresBox.getChildren().add(buildChapitreCard(ch, chTds, chVideos, null));
        }

        updateProgressBar(chapitres.size());
    }

    private void updateProgressBar(int total) {
        if (total <= 0) return;
        int done = completedIds.size();
        // Mettre à jour le label chapitresCountLabel avec la progression
        chapitresCountLabel.setText(done + "/" + total + " chapitre" + (total > 1 ? "s" : "") + " terminé" + (done > 1 ? "s" : ""));
    }

    private VBox buildChapitreCard(Chapitre ch, List<Td> tds, List<VideoExplicative> videos,
                                    java.util.Map<String, String> uiT) {
        // Helper to get translated or default string
        java.util.function.BiFunction<String, String, String> t =
            (key, def) -> (uiT != null && uiT.containsKey(key)) ? uiT.get(key) : def;

        boolean isCompleted = completedIds.contains(ch.getId());

        VBox card = new VBox(0);
        card.getStyleClass().add("card");
        if (isCompleted) card.setStyle("-fx-border-color: #0cbc87; -fx-border-width: 2;");

        // ── Header ──
        HBox header = new HBox(12);
        header.getStyleClass().add("chapitre-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 18, 14, 18));

        // Numéro avec check si terminé
        Label num = new Label(isCompleted ? "✓" : String.valueOf(ch.getOrdre()));
        num.getStyleClass().add("chapitre-num");
        if (isCompleted) num.setStyle("-fx-background-color: #0cbc87; -fx-text-fill: white;");

        Label titre = new Label(safe(ch.getTitre()));
        titre.getStyleClass().add("chapitre-title");
        HBox.setHgrow(titre, Priority.ALWAYS);

        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_RIGHT);
        if (!tds.isEmpty()) {
            Label tdBadge = new Label(tds.size() + " TD");
            tdBadge.getStyleClass().addAll("chip", "chip-warning");
            badges.getChildren().add(tdBadge);
        }
        if (!videos.isEmpty()) {
            Label vidBadge = new Label(videos.size() + " vidéo" + (videos.size() > 1 ? "s" : ""));
            vidBadge.getStyleClass().addAll("chip", "chip-success");
            badges.getChildren().add(vidBadge);
        }
        if (ch.getFichierC() != null && !ch.getFichierC().isBlank()) {
            Button pdfBtn = new Button("⬇ " + t.apply("telecharger", "Chapitre"));
            pdfBtn.getStyleClass().add("btn-rgb-compact");
            pdfBtn.setOnAction(e -> downloadFile(ch.getFichierC(), "chapitre_" + ch.getOrdre() + ".pdf"));
            badges.getChildren().add(pdfBtn);
        }

        Label chevron = new Label("▸");
        chevron.getStyleClass().add("chapitre-chevron");

        header.getChildren().addAll(num, titre, badges, chevron);

        // ── Body ──
        VBox body = new VBox(12);
        body.setPadding(new Insets(0, 18, 16, 18));
        body.setVisible(false);
        body.setManaged(false);

        // Description du chapitre
        String descText = safe(ch.getDescription());
        if (!descText.isBlank()) {
            VBox descBox = new VBox(6);
            descBox.setStyle("-fx-background-color: rgba(6,106,201,0.05); -fx-background-radius: 10px; -fx-border-color: rgba(6,106,201,0.15); -fx-border-radius: 10px; -fx-border-width: 1; -fx-padding: 12 14 12 14;");
            Label descTitle = new Label("📋  " + t.apply("description", "Description"));
            descTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: -edu-primary; -fx-padding: 0 0 2 0;");
            Label descLabel = new Label(descText);
            descLabel.setStyle("-fx-text-fill: -edu-text; -fx-font-size: 13px; -fx-line-spacing: 3px;");
            descLabel.setWrapText(true);
            descBox.getChildren().addAll(descTitle, descLabel);
            body.getChildren().add(descBox);
            javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
            sep.setStyle("-fx-opacity: 0.3; -fx-padding: 4 0 4 0;");
            body.getChildren().add(sep);
        }

        if (!tds.isEmpty()) {
            Label tdSection = new Label(t.apply("tds", "Travaux Dirigés"));
            tdSection.getStyleClass().add("chapitre-section-label");
            body.getChildren().add(tdSection);
            for (Td td : tds) body.getChildren().add(buildTdRow(td, t.apply("telecharger", "Télécharger")));
        }
        if (!videos.isEmpty()) {
            Label vidSection = new Label(t.apply("videos", "Vidéos explicatives"));
            vidSection.getStyleClass().add("chapitre-section-label");
            body.getChildren().add(vidSection);
            for (VideoExplicative v : videos) body.getChildren().add(buildVideoRow(v, t.apply("regarder", "Regarder")));
        }
        if (tds.isEmpty() && videos.isEmpty()) {
            Label noContent = new Label(t.apply("vide", "Aucun contenu pour ce chapitre."));
            noContent.getStyleClass().add("page-subtitle");
            body.getChildren().add(noContent);
        }

        // ── Bouton Terminé ──
        HBox footer = new HBox();
        footer.setPadding(new Insets(8, 18, 14, 18));
        footer.setAlignment(Pos.CENTER_RIGHT);

        javafx.scene.shape.SVGPath checkIcon = new javafx.scene.shape.SVGPath();
        checkIcon.setContent("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z");
        checkIcon.setScaleX(0.7); checkIcon.setScaleY(0.7);

        String luText = "  " + t.apply("lu", "Lu");
        String marquerText = "  " + t.apply("marquerLu", "Marquer comme lu");

        Button doneBtn = new Button(isCompleted ? luText : marquerText);
        doneBtn.setGraphic(checkIcon);
        if (isCompleted) {
            checkIcon.setStyle("-fx-fill: #0cbc87;");
            doneBtn.setStyle("-fx-background-color: rgba(12,188,135,0.12); -fx-text-fill: #0cbc87; -fx-font-weight: 800; -fx-border-color: #0cbc87; -fx-border-width: 1.5; -fx-border-radius: 999px; -fx-background-radius: 999px; -fx-padding: 8 18 8 14; -fx-cursor: hand;");
        } else {
            checkIcon.setStyle("-fx-fill: -edu-primary;");
            doneBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -edu-primary; -fx-font-weight: 700; -fx-border-color: -edu-primary; -fx-border-width: 1.5; -fx-border-radius: 999px; -fx-background-radius: 999px; -fx-padding: 8 18 8 14; -fx-cursor: hand;");
        }

        doneBtn.setOnAction(e -> {
            boolean nowCompleted = !completedIds.contains(ch.getId());
            progressRepo.setCompleted(AppState.getUserId(), ch.getId(), nowCompleted);
            if (nowCompleted) completedIds.add(ch.getId());
            else completedIds.remove(ch.getId());
            List<Chapitre> allChapitres = repo.listChapitresByCoursId(cours.getId());
            updateProgressBar(allChapitres.size());
            if (nowCompleted) {
                card.setStyle("-fx-border-color: #0cbc87; -fx-border-width: 2;");
                num.setText("✓");
                num.setStyle("-fx-background-color: #0cbc87; -fx-text-fill: white;");
                doneBtn.setText(luText);
                checkIcon.setStyle("-fx-fill: #0cbc87;");
                doneBtn.setStyle("-fx-background-color: rgba(12,188,135,0.12); -fx-text-fill: #0cbc87; -fx-font-weight: 800; -fx-border-color: #0cbc87; -fx-border-width: 1.5; -fx-border-radius: 999px; -fx-background-radius: 999px; -fx-padding: 8 18 8 14; -fx-cursor: hand;");
            } else {
                card.setStyle("");
                num.setText(String.valueOf(ch.getOrdre()));
                num.setStyle("");
                doneBtn.setText(marquerText);
                checkIcon.setStyle("-fx-fill: -edu-primary;");
                doneBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -edu-primary; -fx-font-weight: 700; -fx-border-color: -edu-primary; -fx-border-width: 1.5; -fx-border-radius: 999px; -fx-background-radius: 999px; -fx-padding: 8 18 8 14; -fx-cursor: hand;");
            }
        });

        footer.getChildren().add(doneBtn);
        body.getChildren().add(footer);

        // Toggle expand/collapse
        header.setOnMouseClicked(e -> {
            boolean expanded = body.isVisible();
            body.setVisible(!expanded);
            body.setManaged(!expanded);
            chevron.setText(expanded ? "▸" : "▾");
        });
        header.setStyle("-fx-cursor: hand;");

        card.getChildren().addAll(header, body);
        return card;
    }

    private HBox buildTdRow(Td td) {
        return buildTdRow(td, "Télécharger");
    }

    private HBox buildTdRow(Td td, String telechargerLabel) {
        HBox row = new HBox(12);
        row.getStyleClass().add("resource-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        Label icon = new Label("📄");
        icon.setStyle("-fx-font-size: 16px;");
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(safe(td.getTitre()));
        name.getStyleClass().add("resource-name");
        Label desc = new Label(safe(td.getDescription()));
        desc.getStyleClass().add("page-subtitle");
        desc.setWrapText(true);
        if (!desc.getText().isBlank()) info.getChildren().add(desc);
        info.getChildren().add(0, name);
        Button openBtn = new Button("⬇ " + telechargerLabel);
        openBtn.getStyleClass().add("btn-rgb-compact");
        boolean hasFile = td.getFichier() != null && !td.getFichier().isBlank();
        openBtn.setDisable(!hasFile);
        if (hasFile) openBtn.setOnAction(e -> downloadFile(td.getFichier(), "td_" + safe(td.getTitre()).replaceAll("[^a-zA-Z0-9]", "_") + ".pdf"));
        row.getChildren().addAll(icon, info, openBtn);
        return row;
    }

    private HBox buildVideoRow(VideoExplicative video) {
        return buildVideoRow(video, "Regarder");
    }

    private HBox buildVideoRow(VideoExplicative video, String regarderLabel) {
        HBox row = new HBox(12);
        row.getStyleClass().add("resource-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));

        Label icon = new Label("🎬");
        icon.setStyle("-fx-font-size: 16px;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(safe(video.getTitre()));
        name.getStyleClass().add("resource-name");
        info.getChildren().add(name);

        String desc = safe(video.getDescription());
        if (!desc.isBlank()) {
            Label descLabel = new Label(desc);
            descLabel.getStyleClass().add("page-subtitle");
            descLabel.setWrapText(true);
            info.getChildren().add(descLabel);
        }

        String url = safe(video.getUrlVideo());
        boolean isDriveLink = url.contains("drive.google.com");

        // Badge Drive si lien Google Drive
        if (isDriveLink) {
            Label driveBadge = new Label("☁️ Google Drive");
            driveBadge.getStyleClass().addAll("chip", "chip-success");
            driveBadge.setStyle("-fx-font-size: 10px;");
            info.getChildren().add(driveBadge);
        }

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        if (!url.isBlank()) {
            if (isDriveLink) {
                // Bouton principal : ouvrir sur Drive
                Button driveBtn = new Button("☁️ Voir sur Drive");
                driveBtn.getStyleClass().add("btn-rgb");
                driveBtn.setStyle("-fx-font-size: 12px;");
                driveBtn.setTooltip(new Tooltip(url));
                driveBtn.setOnAction(e -> {
                    try { com.educompus.util.UrlOpener.open(url); }
                    catch (Exception ex) { ex.printStackTrace(); }
                });

                // Bouton copier le lien
                Button copyBtn = new Button("📋");
                copyBtn.getStyleClass().add("btn-rgb-outline");
                copyBtn.setTooltip(new Tooltip("Copier le lien Drive"));
                copyBtn.setOnAction(e -> {
                    javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
                    javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                    cc.putString(url);
                    cb.setContent(cc);
                    copyBtn.setText("✅");
                    javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                    p.setOnFinished(ev -> copyBtn.setText("📋"));
                    p.play();
                });

                buttons.getChildren().addAll(driveBtn, copyBtn);
            } else if (isVideoUrl(url)) {
                Button playBtn = new Button("▶ Regarder");
                playBtn.getStyleClass().add("btn-rgb-compact");
                playBtn.setTooltip(new Tooltip(url));
                playBtn.setOnAction(e -> openVideoInApp(url, safe(video.getTitre())));
                buttons.getChildren().add(playBtn);
            } else {
                Button openBtn = new Button("▶ Regarder");
                openBtn.getStyleClass().add("btn-rgb-compact");
                openBtn.setTooltip(new Tooltip(url));
                openBtn.setOnAction(e -> openUrl(url));
                buttons.getChildren().add(openBtn);
            }
        } else {
            // Pas d'URL : générer une vidéo IA contextuelle
            Button genBtn = new Button("🤖 Générer vidéo");
            genBtn.getStyleClass().add("btn-rgb-compact");
            genBtn.setTooltip(new Tooltip("Générer et lire une vidéo IA contextuelle"));
            genBtn.setOnAction(e -> genererEtOuvrirVideoContextuelle(video));
            buttons.getChildren().add(genBtn);
        }

        row.getChildren().addAll(icon, info, buttons);
        return row;
    }

    /**
     * Génère et ouvre une vidéo contextuelle avec audio basée sur les informations du chapitre.
     * Résout le problème critique de l'audio manquant dans les vidéos générées.
     */
    private void genererEtOuvrirVideoContextuelle(VideoExplicative video) {
        // Afficher un indicateur de chargement
        javafx.scene.control.Alert loadingAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Génération vidéo IA");
        loadingAlert.setHeaderText("Génération en cours...");
        loadingAlert.setContentText("Création d'une vidéo contextuelle avec synthèse vocale.\nVeuillez patienter...");
        
        // Rendre l'alerte non-modale et l'afficher
        loadingAlert.initModality(javafx.stage.Modality.NONE);
        loadingAlert.show();
        
        // Exécuter la génération dans un thread séparé pour ne pas bloquer l'UI
        Task<String> generationTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                // Obtenir les informations contextuelles du chapitre
                String titreChapitre = null;
                String descriptionChapitre = null;
                String niveau = cours != null ? cours.getNiveau() : null;
                String domaine = cours != null ? cours.getDomaine() : null;
                
                // Trouver le chapitre parent de cette vidéo
                if (cours != null && video.getChapitreId() > 0) {
                    try {
                        List<com.educompus.model.Chapitre> chapitres = repo.listChapitresByCoursId(cours.getId());
                        for (com.educompus.model.Chapitre ch : chapitres) {
                            if (ch.getId() == video.getChapitreId()) {
                                titreChapitre = ch.getTitre();
                                descriptionChapitre = ch.getDescription();
                                break;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la récupération du chapitre: " + e.getMessage());
                    }
                }
                
                // Générer la vidéo contextuelle avec audio
                String cheminVideo = com.educompus.service.LocalVideoGeneratorService.genererVideoContextuelle(
                    String.valueOf(video.getId()),
                    safe(video.getTitre()),
                    safe(video.getDescription()),
                    titreChapitre,
                    descriptionChapitre,
                    niveau,
                    domaine,
                    "Assistant IA EduCompus", // Avatar par défaut
                    "Synthèse vocale française" // Voix par défaut
                );
                
                return cheminVideo;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    loadingAlert.close();
                    
                    String cheminVideo = getValue();
                    if (cheminVideo != null && !cheminVideo.isBlank()) {
                        // Ouvrir la vidéo générée dans le lecteur intégré
                        openVideoInApp("file:///" + cheminVideo.replace("\\", "/"), safe(video.getTitre()));
                        
                        // Afficher un message de succès
                        javafx.scene.control.Alert successAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        successAlert.setTitle("Vidéo générée");
                        successAlert.setHeaderText("Succès !");
                        successAlert.setContentText("Vidéo contextuelle avec audio générée et ouverte dans le lecteur intégré.\n\n" +
                                                   "Fichier: " + cheminVideo);
                        successAlert.showAndWait();
                    } else {
                        // Afficher un message d'erreur
                        javafx.scene.control.Alert errorAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                        errorAlert.setTitle("Erreur de génération");
                        errorAlert.setHeaderText("Impossible de générer la vidéo");
                        errorAlert.setContentText("La génération de la vidéo contextuelle a échoué.\n\n" +
                                                 "Vérifiez que FFmpeg est installé et accessible depuis le PATH système.");
                        errorAlert.showAndWait();
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    loadingAlert.close();
                    
                    javafx.scene.control.Alert errorAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    errorAlert.setTitle("Erreur de génération");
                    errorAlert.setHeaderText("Échec de la génération vidéo");
                    errorAlert.setContentText("Une erreur s'est produite lors de la génération de la vidéo contextuelle.\n\n" +
                                             "Erreur: " + (getException() != null ? getException().getMessage() : "Inconnue"));
                    errorAlert.showAndWait();
                });
            }
        };
        
        // Lancer la tâche dans un thread séparé
        Thread generationThread = new Thread(generationTask);
        generationThread.setDaemon(true);
        generationThread.start();
    }

    @FXML
    private void onBack() {
        // Naviguer dans le contentWrap du shell sans remplacer tout le shell
        try {
            javafx.scene.Node current = chapitresBox;
            while (current != null) {
                if (current instanceof javafx.scene.layout.StackPane sp && "contentWrap".equals(sp.getId())) {
                    javafx.scene.Parent coursesRoot = Navigator.load("View/front/FrontCourses.fxml");
                    sp.getChildren().setAll(coursesRoot);
                    return;
                }
                current = current.getParent();
            }
        } catch (Exception ignored) {}
        // fallback
        Navigator.goRoot("View/front/FrontCourses.fxml");
    }

    private void openFile(String path) {
        if (path == null || path.isBlank()) return;
        try {
            File file = new File(path);
            if (file.exists()) Desktop.getDesktop().open(file);
        } catch (Exception ignored) {}
    }

    /** Ouvre le fichier avec l'application par défaut (lecture/téléchargement). */
    private void downloadFile(String path, String suggestedName) {
        if (path == null || path.isBlank()) return;
        File file = new File(path);
        if (!file.exists()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Fichier introuvable");
            alert.setHeaderText(null);
            alert.setContentText("Le fichier est introuvable :\n" + path);
            alert.showAndWait();
            return;
        }
        try {
            // Ouvrir avec l'application par défaut (PDF viewer, etc.)
            Desktop.getDesktop().open(file);
        } catch (Exception e) {
            // Fallback : copier dans le dossier Téléchargements
            try {
                File dest = new File(System.getProperty("user.home") + "/Downloads/" + suggestedName);
                java.nio.file.Files.copy(file.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Téléchargement");
                alert.setHeaderText(null);
                alert.setContentText("Fichier copié dans :\n" + dest.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception ex) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setContentText("Impossible d'ouvrir le fichier : " + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void openUrl(String url) {
        if (url == null || url.isBlank()) return;
        
        // CORRECTION DU BUG: Vérifier si c'est une URL vidéo et utiliser le lecteur intégré
        if (isVideoUrl(url)) {
            // Utiliser le lecteur vidéo intégré pour les vidéos
            openVideoInApp(url, "Lecture vidéo");
            return;
        }
        
        // Pour les URLs non-vidéo, utiliser UrlOpener comme avant
        try {
            com.educompus.util.UrlOpener.open(url);
        } catch (Exception e) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erreur ouverture URL");
            alert.setHeaderText("Impossible d'ouvrir l'URL");
            alert.setContentText("URL : " + url + "\n\nErreur : " + e.getMessage());
            alert.showAndWait();
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // ── Méthodes Widget Mini Traducteur ──────────────────────────────────────

    @FXML
    private void onCloseMiniTranslator() {
        if (miniTranslatorWidget != null) {
            miniTranslatorWidget.setVisible(false);
            miniTranslatorWidget.setManaged(false);
        }
    }

    @FXML
    private void onMiniSwapLangs() {
        if (miniSourceLang == null || miniTargetLang == null) return;
        MyMemoryTranslationService.Language temp = miniSourceLang.getValue();
        miniSourceLang.setValue(miniTargetLang.getValue());
        miniTargetLang.setValue(temp);
    }

    @FXML
    private void onMiniTranslate() {
        if (miniSourceText == null || miniResultText == null) return;
        String text = miniSourceText.getText();
        if (text == null || text.isBlank()) {
            miniResultText.setText("");
            return;
        }

        MyMemoryTranslationService.Language sourceLang = miniSourceLang.getValue();
        MyMemoryTranslationService.Language targetLang = miniTargetLang.getValue();
        if (sourceLang == null || targetLang == null) return;

        if (sourceLang == targetLang) {
            miniResultText.setText(text);
            showMiniStatus("✓ Même langue");
            return;
        }

        setMiniTranslating(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return MyMemoryTranslationService.translate(text, sourceLang.code, targetLang.code);
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    miniResultText.setText(getValue());
                    setMiniTranslating(false);
                    showMiniStatus("✓ Traduit");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    setMiniTranslating(false);
                    showMiniStatus("⚠ Erreur");
                });
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onMiniCopy() {
        if (miniResultText == null || miniResultText.getText() == null || miniResultText.getText().isBlank()) return;
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(miniResultText.getText());
        clipboard.setContent(content);
        showMiniStatus("✓ Copié");
    }

    private void setMiniTranslating(boolean loading) {
        if (miniSpinner != null) {
            miniSpinner.setVisible(loading);
            miniSpinner.setManaged(loading);
        }
        if (btnMiniTranslate != null) btnMiniTranslate.setDisable(loading);
    }

    private void showMiniStatus(String msg) {
        if (miniStatus == null) return;
        miniStatus.setText(msg);
        miniStatus.setVisible(true);
        miniStatus.setManaged(true);
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        pause.setOnFinished(e -> {
            miniStatus.setVisible(false);
            miniStatus.setManaged(false);
        });
        pause.play();
    }

    // ── Méthodes pour le Lecteur Vidéo Intégré ──────────────────────────────

    /**
     * Détermine si une URL correspond à un fichier vidéo ou un service de streaming vidéo.
     * Détecte les extensions vidéo et les services comme Vimeo, YouTube, etc.
     */
    private boolean isVideoUrl(String url) {
        if (url == null || url.isBlank()) return false;
        String lowerUrl = url.toLowerCase();
        
        // Extensions de fichiers vidéo
        if (lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".avi") || 
            lowerUrl.endsWith(".mov") || lowerUrl.endsWith(".wmv") || 
            lowerUrl.endsWith(".webm") || lowerUrl.endsWith(".mkv") ||
            lowerUrl.endsWith(".flv") || lowerUrl.endsWith(".m4v") ||
            lowerUrl.endsWith(".3gp") || lowerUrl.endsWith(".ogv")) {
            return true;
        }
        
        // Services de streaming vidéo
        if (lowerUrl.contains("player.vimeo.com") ||
            lowerUrl.contains("youtube.com/watch") ||
            lowerUrl.contains("youtu.be/") ||
            lowerUrl.contains("dailymotion.com") ||
            lowerUrl.contains("twitch.tv") ||
            lowerUrl.contains("storage.googleapis.com") && lowerUrl.contains("video")) {
            return true;
        }
        
        // URLs contenant "video" dans le chemin (heuristique)
        if (lowerUrl.contains("/video/") || lowerUrl.contains("/videos/")) {
            return true;
        }
        
        return false;
    }

    /**
     * Ouvre une vidéo dans le lecteur intégré MediaView.
     */
    private void openVideoInApp(String url, String title) {
        if (url == null || url.isBlank()) return;
        
        // DIAGNOSTIC: Vérifier l'état avant ouverture vidéo
        boolean composantsOK = com.educompus.debug.VideoPlayerDiagnostic.verifierComposantsInitialises(this);
        com.educompus.debug.VideoPlayerDiagnostic.logDiagnosticRapide("OPEN_VIDEO_IN_APP", composantsOK);
        
        try {
            // VÉRIFICATION CRITIQUE : S'assurer que les composants FXML sont initialisés
            if (videoModal == null || mediaView == null) {
                System.err.println("⚠️ COMPOSANTS FXML NON INITIALISÉS - Fallback vers navigateur externe");
                System.err.println("   videoModal: " + (videoModal != null ? "OK" : "NULL"));
                System.err.println("   mediaView: " + (mediaView != null ? "OK" : "NULL"));
                System.err.println("   Cela peut indiquer un problème de chargement FXML");
                System.err.println("   Contexte: Rôle=" + AppState.getRole() + ", User=" + AppState.getUserId());
                
                // Fallback vers UrlOpener pour éviter un écran noir
                com.educompus.util.UrlOpener.open(url);
                return;
            }
            
            // Nettoyer le lecteur précédent s'il existe
            if (currentMediaPlayer != null) {
                currentMediaPlayer.stop();
                currentMediaPlayer.dispose();
                currentMediaPlayer = null;
            }

            // Créer le nouveau Media et MediaPlayer
            Media media = new Media(url);
            currentMediaPlayer = new MediaPlayer(media);
            
            // Configurer MediaView
            if (mediaView != null) {
                mediaView.setMediaPlayer(currentMediaPlayer);
            }
            
            // Configurer le titre
            if (videoTitle != null) {
                videoTitle.setText(title != null && !title.isBlank() ? title : "Lecture vidéo");
            }
            
            // Configurer les contrôles
            setupVideoControls();
            
            // Afficher le modal
            if (videoModal != null) {
                videoModal.setVisible(true);
                videoModal.setManaged(true);
                videoModal.toFront();
            }
            
            // Démarrer la lecture automatique
            currentMediaPlayer.setAutoPlay(true);
            isPlaying = true;
            updatePlayPauseButton();
            
            System.out.println("✅ Vidéo ouverte dans le lecteur intégré: " + title);
            
        } catch (Exception e) {
            // En cas d'erreur, fallback vers UrlOpener
            System.err.println("❌ Erreur lors de l'ouverture de la vidéo: " + e.getMessage());
            e.printStackTrace();
            
            try {
                com.educompus.util.UrlOpener.open(url);
                System.out.println("🔄 Fallback vers navigateur externe réussi");
            } catch (Exception fallbackError) {
                System.err.println("❌ Échec du fallback: " + fallbackError.getMessage());
            }
        }
    }

    /**
     * Configure les contrôles du lecteur vidéo.
     */
    private void setupVideoControls() {
        if (currentMediaPlayer == null) return;
        
        // Configurer le slider de volume
        if (volumeSlider != null) {
            volumeSlider.setValue(currentMediaPlayer.getVolume());
            volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (currentMediaPlayer != null) {
                    currentMediaPlayer.setVolume(newVal.doubleValue());
                }
            });
        }
        
        // Configurer le slider de progression
        if (progressSlider != null && timeLabel != null) {
            currentMediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!progressSlider.isValueChanging()) {
                    Duration totalDuration = currentMediaPlayer.getTotalDuration();
                    if (totalDuration != null && totalDuration.greaterThan(Duration.ZERO)) {
                        double progress = newTime.toMillis() / totalDuration.toMillis() * 100.0;
                        progressSlider.setValue(progress);
                        
                        // Mettre à jour le label de temps
                        String currentTimeStr = formatDuration(newTime);
                        String totalTimeStr = formatDuration(totalDuration);
                        timeLabel.setText(currentTimeStr + " / " + totalTimeStr);
                    }
                }
            });
            
            progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (progressSlider.isValueChanging() && currentMediaPlayer != null) {
                    Duration totalDuration = currentMediaPlayer.getTotalDuration();
                    if (totalDuration != null) {
                        Duration seekTime = totalDuration.multiply(newVal.doubleValue() / 100.0);
                        currentMediaPlayer.seek(seekTime);
                    }
                }
            });
        }
        
        // Écouter les changements d'état du lecteur
        currentMediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
            Platform.runLater(() -> {
                switch (newStatus) {
                    case PLAYING:
                        isPlaying = true;
                        updatePlayPauseButton();
                        break;
                    case PAUSED:
                    case STOPPED:
                        isPlaying = false;
                        updatePlayPauseButton();
                        break;
                    case READY:
                        // Vidéo prête, on peut afficher la durée totale
                        if (timeLabel != null) {
                            Duration totalDuration = currentMediaPlayer.getTotalDuration();
                            if (totalDuration != null) {
                                timeLabel.setText("00:00 / " + formatDuration(totalDuration));
                            }
                        }
                        break;
                    default:
                        break;
                }
            });
        });
    }

    /**
     * Formate une durée en format MM:SS.
     */
    private String formatDuration(Duration duration) {
        if (duration == null) return "00:00";
        int minutes = (int) duration.toMinutes();
        int seconds = (int) (duration.toSeconds() % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Met à jour le texte du bouton play/pause.
     */
    private void updatePlayPauseButton() {
        if (playPauseBtn != null) {
            playPauseBtn.setText(isPlaying ? "⏸" : "▶");
        }
    }

    @FXML
    private void onPlayPause() {
        if (currentMediaPlayer == null) return;
        
        if (isPlaying) {
            currentMediaPlayer.pause();
        } else {
            currentMediaPlayer.play();
        }
    }

    @FXML
    private void onCloseVideo() {
        // Arrêter et nettoyer le lecteur
        if (currentMediaPlayer != null) {
            currentMediaPlayer.stop();
            currentMediaPlayer.dispose();
            currentMediaPlayer = null;
        }
        
        // Masquer le modal
        if (videoModal != null) {
            videoModal.setVisible(false);
            videoModal.setManaged(false);
        }
        
        // Réinitialiser les contrôles
        isPlaying = false;
        if (playPauseBtn != null) playPauseBtn.setText("▶");
        if (timeLabel != null) timeLabel.setText("00:00 / 00:00");
        if (progressSlider != null) progressSlider.setValue(0);
    }

    @FXML
    private void onToggleFullscreen() {
        // Pour l'instant, on peut juste agrandir la fenêtre ou afficher un message
        // L'implémentation complète du plein écran nécessiterait plus de travail
        if (mediaView != null) {
            // Basculer entre taille normale et agrandie
            if (mediaView.getFitWidth() == 760) {
                mediaView.setFitWidth(1200);
                mediaView.setFitHeight(675);
            } else {
                mediaView.setFitWidth(760);
                mediaView.setFitHeight(428);
            }
        }
    }
}

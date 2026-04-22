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
import javafx.scene.layout.VBox;

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

                Platform.runLater(() -> {
                    currentLang = targetLang;
                    // Mettre à jour titre et description du cours
                    coursTitle.setText(newTitre);
                    coursDescription.setText(newDesc);
                    if (breadcrumb != null) breadcrumb.setText(newTitre);

                    // Reconstruire les cartes chapitres avec traduction complète
                    chapitresBox.getChildren().clear();
                    if (chapitres.isEmpty()) {
                        Label empty = new Label("Aucun chapitre disponible.");
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
                            
                            chapitresBox.getChildren().add(buildChapitreCard(translated, chTds, chVideos));
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

        if (cours.getDriveFolderId() != null && !cours.getDriveFolderId().equals("EN_ATTENTE") && !cours.getDriveFolderId().isBlank()) {
            if (driveCard != null) {
                driveCard.setVisible(true);
                driveCard.setManaged(true);
            }
            if (btnOpenDrive != null) {
                btnOpenDrive.setOnAction(e -> openUrl("https://drive.google.com/drive/folders/" + cours.getDriveFolderId()));
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
            chapitresBox.getChildren().add(buildChapitreCard(ch, chTds, chVideos));
        }

        updateProgressBar(chapitres.size());
    }

    private void updateProgressBar(int total) {
        if (total <= 0) return;
        int done = completedIds.size();
        // Mettre à jour le label chapitresCountLabel avec la progression
        chapitresCountLabel.setText(done + "/" + total + " chapitre" + (total > 1 ? "s" : "") + " terminé" + (done > 1 ? "s" : ""));
    }

    private VBox buildChapitreCard(Chapitre ch, List<Td> tds, List<VideoExplicative> videos) {
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
            Button pdfBtn = new Button("⬇ Chapitre");
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
            Label descTitle = new Label("📋  Description");
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
            Label tdSection = new Label("Travaux Dirigés");
            tdSection.getStyleClass().add("chapitre-section-label");
            body.getChildren().add(tdSection);
            for (Td td : tds) body.getChildren().add(buildTdRow(td));
        }
        if (!videos.isEmpty()) {
            Label vidSection = new Label("Vidéos explicatives");
            vidSection.getStyleClass().add("chapitre-section-label");
            body.getChildren().add(vidSection);
            for (VideoExplicative v : videos) body.getChildren().add(buildVideoRow(v));
        }
        if (tds.isEmpty() && videos.isEmpty()) {
            Label noContent = new Label("Aucun contenu pour ce chapitre.");
            noContent.getStyleClass().add("page-subtitle");
            body.getChildren().add(noContent);
        }

        // ── Bouton Terminé ──
        HBox footer = new HBox();
        footer.setPadding(new Insets(8, 18, 14, 18));
        footer.setAlignment(Pos.CENTER_RIGHT);

        // Icône SVG check
        javafx.scene.shape.SVGPath checkIcon = new javafx.scene.shape.SVGPath();
        checkIcon.setContent("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z");
        checkIcon.setScaleX(0.7); checkIcon.setScaleY(0.7);

        Button doneBtn = new Button(isCompleted ? "  Lu" : "  Marquer comme lu");
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
            // Rafraîchir la vue
            List<Chapitre> allChapitres = repo.listChapitresByCoursId(cours.getId());
            updateProgressBar(allChapitres.size());
            // Mettre à jour visuellement cette carte
            if (nowCompleted) {
                card.setStyle("-fx-border-color: #0cbc87; -fx-border-width: 2;");
                num.setText("✓");
                num.setStyle("-fx-background-color: #0cbc87; -fx-text-fill: white;");
                doneBtn.setText("  Lu");
                checkIcon.setStyle("-fx-fill: #0cbc87;");
                doneBtn.setStyle("-fx-background-color: rgba(12,188,135,0.12); -fx-text-fill: #0cbc87; -fx-font-weight: 800; -fx-border-color: #0cbc87; -fx-border-width: 1.5; -fx-border-radius: 999px; -fx-background-radius: 999px; -fx-padding: 8 18 8 14; -fx-cursor: hand;");
            } else {
                card.setStyle("");
                num.setText(String.valueOf(ch.getOrdre()));
                num.setStyle("");
                doneBtn.setText("  Marquer comme lu");
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
        Button openBtn = new Button("⬇ Télécharger");
        openBtn.getStyleClass().add("btn-rgb-compact");
        boolean hasFile = td.getFichier() != null && !td.getFichier().isBlank();
        openBtn.setDisable(!hasFile);
        if (hasFile) openBtn.setOnAction(e -> downloadFile(td.getFichier(), "td_" + safe(td.getTitre()).replaceAll("[^a-zA-Z0-9]", "_") + ".pdf"));
        row.getChildren().addAll(icon, info, openBtn);
        return row;
    }

    private HBox buildVideoRow(VideoExplicative video) {
        HBox row = new HBox(12);
        row.getStyleClass().add("resource-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        Label icon = new Label("🎬");
        icon.setStyle("-fx-font-size: 16px;");
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(safe(video.getTitre()));
        name.getStyleClass().add("resource-name");
        Label desc = new Label(safe(video.getDescription()));
        desc.getStyleClass().add("page-subtitle");
        desc.setWrapText(true);
        if (!desc.getText().isBlank()) info.getChildren().add(desc);
        info.getChildren().add(0, name);
        String url = safe(video.getUrlVideo());
        Button openBtn = new Button("▶ Regarder");
        openBtn.getStyleClass().add("btn-rgb-compact");
        openBtn.setDisable(url.isBlank());
        if (!url.isBlank()) {
            openBtn.setTooltip(new Tooltip(url));
            openBtn.setOnAction(e -> openUrl(url));
        }
        row.getChildren().addAll(icon, info, openBtn);
        return row;
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
        try {
            com.educompus.util.UrlOpener.open(url);
        } catch (Exception e) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erreur ouverture vidéo");
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
}

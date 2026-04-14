package com.educompus.controller.front;

import com.educompus.model.Chapitre;
import com.educompus.model.Cours;
import com.educompus.model.Td;
import com.educompus.model.VideoExplicative;
import com.educompus.repository.CourseManagementRepository;
import com.educompus.nav.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.List;

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
    @FXML private VBox chapitresBox;

    private final CourseManagementRepository repo = new CourseManagementRepository();
    private Cours cours;

    /** Appelé par FrontCoursesController avant de naviguer vers cette vue */
    public void setCours(Cours c) {
        this.cours = c;
        populate();
    }

    @FXML
    private void initialize() {
        // populate() sera appelé via setCours()
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

        if (cours.getDateCreation() != null) {
            dateLabel.setText("Créé le " + cours.getDateCreation().toString().substring(0, 10));
        }

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
    }

    private VBox buildChapitreCard(Chapitre ch, List<Td> tds, List<VideoExplicative> videos) {
        VBox card = new VBox(0);
        card.getStyleClass().add("card");

        // ── Header (cliquable pour expand/collapse) ──
        HBox header = new HBox(12);
        header.getStyleClass().add("chapitre-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 18, 14, 18));

        // Numéro
        Label num = new Label(String.valueOf(ch.getOrdre()));
        num.getStyleClass().add("chapitre-num");

        // Titre
        Label titre = new Label(safe(ch.getTitre()));
        titre.getStyleClass().add("chapitre-title");
        HBox.setHgrow(titre, Priority.ALWAYS);

        // Badges TD + vidéos
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

        // Bouton PDF chapitre
        if (ch.getFichierC() != null && !ch.getFichierC().isBlank()) {
            Button pdfBtn = new Button("PDF");
            pdfBtn.getStyleClass().add("btn-rgb-compact");
            pdfBtn.setOnAction(e -> openFile(ch.getFichierC()));
            badges.getChildren().add(pdfBtn);
        }

        // Chevron toggle
        Label chevron = new Label("▸");
        chevron.getStyleClass().add("chapitre-chevron");

        header.getChildren().addAll(num, titre, badges, chevron);

        // ── Body (TDs + vidéos) ──
        VBox body = new VBox(10);
        body.setPadding(new Insets(0, 18, 14, 18));
        body.setVisible(false);
        body.setManaged(false);

        if (!tds.isEmpty()) {
            Label tdSection = new Label("Travaux Dirigés");
            tdSection.getStyleClass().add("chapitre-section-label");
            body.getChildren().add(tdSection);
            for (Td td : tds) {
                body.getChildren().add(buildTdRow(td));
            }
        }

        if (!videos.isEmpty()) {
            Label vidSection = new Label("Vidéos explicatives");
            vidSection.getStyleClass().add("chapitre-section-label");
            body.getChildren().add(vidSection);
            for (VideoExplicative v : videos) {
                body.getChildren().add(buildVideoRow(v));
            }
        }

        if (tds.isEmpty() && videos.isEmpty()) {
            Label noContent = new Label("Aucun contenu pour ce chapitre.");
            noContent.getStyleClass().add("page-subtitle");
            body.getChildren().add(noContent);
        }

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

        // Icône TD
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

        Button openBtn = new Button("Ouvrir");
        openBtn.getStyleClass().add("btn-rgb-compact");
        boolean hasFile = td.getFichier() != null && !td.getFichier().isBlank();
        openBtn.setDisable(!hasFile);
        if (hasFile) openBtn.setOnAction(e -> openFile(td.getFichier()));

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
            openBtn.setTooltip(new javafx.scene.control.Tooltip(url));
            openBtn.setOnAction(e -> openUrl(url));
        }

        row.getChildren().addAll(icon, info, openBtn);
        return row;
    }

    @FXML
    private void onBack() {
        Navigator.goRoot("View/front/FrontCourses.fxml");
    }

    private void openFile(String path) {
        if (path == null || path.isBlank()) return;
        try {
            File file = new File(path);
            if (file.exists()) Desktop.getDesktop().open(file);
        } catch (Exception ignored) {}
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
}

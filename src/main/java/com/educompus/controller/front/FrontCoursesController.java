package com.educompus.controller.front;

import com.educompus.model.Cours;
import com.educompus.repository.CourseManagementRepository;
import com.educompus.nav.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;

public final class FrontCoursesController {
    private final CourseManagementRepository repository = new CourseManagementRepository();

    @FXML private FlowPane cardsFlow;
    @FXML private Label totalCoursesLabel;
    @FXML private TextField searchField;

    private List<Cours> allCourses;

    @FXML
    private void initialize() {
        allCourses = repository.listCours("");
        if (totalCoursesLabel != null) totalCoursesLabel.setText(String.valueOf(allCourses.size()));
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> filterAndRender(n));
        }
        renderCards(allCourses);
    }

    private void filterAndRender(String query) {
        if (query == null || query.isBlank()) {
            renderCards(allCourses);
            return;
        }
        String q = query.trim().toLowerCase();
        List<Cours> filtered = allCourses.stream()
            .filter(c -> safe(c.getTitre()).toLowerCase().contains(q)
                      || safe(c.getDomaine()).toLowerCase().contains(q)
                      || safe(c.getNomFormateur()).toLowerCase().contains(q))
            .toList();
        renderCards(filtered);
    }

    private void renderCards(List<Cours> courses) {
        if (cardsFlow == null) return;
        cardsFlow.getChildren().clear();
        if (courses.isEmpty()) {
            Label empty = new Label("Aucun cours disponible.");
            empty.getStyleClass().add("empty-title");
            cardsFlow.getChildren().add(empty);
            return;
        }
        for (Cours cours : courses) {
            cardsFlow.getChildren().add(buildCard(cours));
        }
    }

    private VBox buildCard(Cours cours) {
        VBox card = new VBox(0);
        card.getStyleClass().add("course-catalog-card");
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setStyle("-fx-cursor: hand;");
        card.setOnMouseClicked(e -> openDetail(cours));

        // ── Image banner ──
        StackPane banner = new StackPane();
        banner.getStyleClass().add("course-catalog-banner");
        banner.setPrefHeight(160);
        banner.setMinHeight(160);
        banner.setMaxHeight(160);

        ImageView iv = new ImageView(loadCourseImage(cours));
        iv.setFitWidth(280);
        iv.setFitHeight(160);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);

        // Chip domaine en haut à droite
        Label domainChip = new Label(safe(cours.getDomaine()).isBlank() ? "Général" : safe(cours.getDomaine()));
        domainChip.getStyleClass().addAll("chip", "chip-info");
        domainChip.setStyle("-fx-background-color: rgba(6,106,201,0.85); -fx-text-fill: white; -fx-font-weight: 700;");
        StackPane.setAlignment(domainChip, Pos.TOP_RIGHT);
        StackPane.setMargin(domainChip, new Insets(10));

        // Chip niveau en haut à gauche
        Label niveauChip = new Label(safe(cours.getNiveau()).isBlank() ? "" : safe(cours.getNiveau()));
        niveauChip.getStyleClass().addAll("chip", "chip-warning");
        StackPane.setAlignment(niveauChip, Pos.TOP_LEFT);
        StackPane.setMargin(niveauChip, new Insets(10));

        banner.getChildren().addAll(iv, domainChip);
        if (!niveauChip.getText().isBlank()) banner.getChildren().add(niveauChip);

        // ── Corps de la carte ──
        VBox body = new VBox(8);
        body.setPadding(new Insets(14, 16, 14, 16));

        Label title = new Label(safe(cours.getTitre()));
        title.getStyleClass().add("course-catalog-title");
        title.setWrapText(true);

        Label desc = new Label(safe(cours.getDescription()));
        desc.getStyleClass().add("project-card-subtitle");
        desc.setWrapText(true);
        desc.setMaxHeight(40);
        String d = safe(cours.getDescription());
        desc.setText(d.length() > 80 ? d.substring(0, 78) + "…" : d);

        // ── Footer ──
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("course-catalog-footer");

        Label formateur = new Label("👤 " + (safe(cours.getNomFormateur()).isBlank() ? "N/A" : safe(cours.getNomFormateur())));
        formateur.getStyleClass().add("stat-title");
        formateur.setStyle("-fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label duree = new Label("⏱ " + cours.getDureeTotaleHeures() + "h");
        duree.getStyleClass().add("stat-title");
        duree.setStyle("-fx-font-size: 11px;");

        Label chapitres = new Label("📚 " + cours.getChapitreCount() + " chap.");
        chapitres.getStyleClass().add("stat-title");
        chapitres.setStyle("-fx-font-size: 11px;");

        footer.getChildren().addAll(formateur, spacer, duree, chapitres);
        body.getChildren().addAll(title, desc, footer);
        card.getChildren().addAll(banner, body);
        return card;
    }

    private Image loadCourseImage(Cours cours) {
        // 1. Image réelle depuis la base (chemin absolu ou relatif)
        String imgPath = safe(cours.getImage());
        if (!imgPath.isBlank() && !imgPath.startsWith("auto:")) {
            // Chemin fichier local
            File f = new File(imgPath);
            if (f.exists()) {
                try { return new Image(f.toURI().toString(), 280, 160, false, true); }
                catch (Exception ignored) {}
            }
            // URL http/https
            if (imgPath.startsWith("http://") || imgPath.startsWith("https://")) {
                try { return new Image(imgPath, 280, 160, false, true, true); }
                catch (Exception ignored) {}
            }
            // Ressource classpath
            var res = getClass().getResource(imgPath.startsWith("/") ? imgPath : "/" + imgPath);
            if (res != null) {
                try { return new Image(res.toExternalForm(), 280, 160, false, true); }
                catch (Exception ignored) {}
            }
        }

        // 2. Fallback par domaine
        String domain = safe(cours.getDomaine()).toLowerCase();
        String path;
        if (domain.contains("informatique") || domain.contains("développement") || domain.contains("ia") || domain.contains("intelligence")) {
            path = "/assets/images/02.9bb56457.png";
        } else if (domain.contains("math") || domain.contains("physique") || domain.contains("data")) {
            path = "/assets/images/ss.png";
        } else {
            path = "/assets/images/app-icon.png";
        }
        var url = getClass().getResource(path);
        return new Image(url == null ? "" : url.toExternalForm(), 280, 160, false, true);
    }

    private void openDetail(Cours cours) {
        try {
            javafx.fxml.FXMLLoader loader = Navigator.loader("View/front/FrontCourseDetail.fxml");
            javafx.scene.Parent root = loader.load();
            FrontCourseDetailController ctrl = loader.getController();
            ctrl.setCours(cours);
            javafx.scene.Scene scene = cardsFlow.getScene();
            if (scene != null) scene.setRoot(root);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

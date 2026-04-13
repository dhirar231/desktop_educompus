package com.educompus.controller.front;

import com.educompus.model.Cours;
import com.educompus.repository.CourseManagementRepository;
import com.educompus.nav.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
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
import java.util.stream.Collectors;

public final class FrontCoursesController {
    private final CourseManagementRepository repository = new CourseManagementRepository();

    @FXML private FlowPane cardsFlow;
    @FXML private Label totalCoursesLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private VBox emptyState;

    private List<Cours> allCourses;

    @FXML
    private void initialize() {
        allCourses = repository.listCours("");
        if (totalCoursesLabel != null) totalCoursesLabel.setText(String.valueOf(allCourses.size()));

        // Remplir le filtre domaine
        if (filterCombo != null) {
            filterCombo.getItems().add("Tous les domaines");
            allCourses.stream()
                .map(c -> safe(c.getDomaine()))
                .filter(d -> !d.isBlank())
                .distinct()
                .sorted()
                .forEach(filterCombo.getItems()::add);
            filterCombo.setValue("Tous les domaines");
            filterCombo.valueProperty().addListener((obs, o, n) -> applyFilter());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        }

        renderCards(allCourses);
    }

    @FXML
    private void onReset() {
        if (searchField != null) searchField.clear();
        if (filterCombo != null) filterCombo.setValue("Tous les domaines");
        renderCards(allCourses);
    }

    private void applyFilter() {
        String q = searchField == null ? "" : safe(searchField.getText()).toLowerCase();
        String domaine = filterCombo == null ? "" : safe(filterCombo.getValue());

        List<Cours> filtered = allCourses.stream()
            .filter(c -> {
                boolean matchQ = q.isBlank()
                    || safe(c.getTitre()).toLowerCase().contains(q)
                    || safe(c.getDomaine()).toLowerCase().contains(q)
                    || safe(c.getNomFormateur()).toLowerCase().contains(q);
                boolean matchD = domaine.isBlank() || domaine.equals("Tous les domaines")
                    || safe(c.getDomaine()).equalsIgnoreCase(domaine);
                return matchQ && matchD;
            })
            .collect(Collectors.toList());

        renderCards(filtered);
    }

    private void renderCards(List<Cours> courses) {
        if (cardsFlow == null) return;
        cardsFlow.getChildren().clear();

        boolean empty = courses.isEmpty();
        if (emptyState != null) {
            emptyState.setVisible(empty);
            emptyState.setManaged(empty);
        }

        for (Cours cours : courses) {
            cardsFlow.getChildren().add(buildCard(cours));
        }
    }

    private VBox buildCard(Cours cours) {
        VBox card = new VBox(0);
        card.getStyleClass().add("project-card");
        card.setPrefWidth(270);
        card.setMaxWidth(270);
        card.setStyle("-fx-cursor: hand;");
        card.setOnMouseClicked(e -> openDetail(cours));

        // ── Bannière image ──
        StackPane banner = new StackPane();
        banner.setMinHeight(150);
        banner.setPrefHeight(150);
        banner.setMaxHeight(150);
        banner.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(6,106,201,0.15), rgba(106,17,203,0.10)); -fx-background-radius: 16px 16px 0 0;");

        ImageView iv = new ImageView(loadCourseImage(cours));
        iv.setFitWidth(270);
        iv.setFitHeight(150);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);

        // Chip domaine
        Label domainChip = new Label(safe(cours.getDomaine()).isBlank() ? "Général" : safe(cours.getDomaine()));
        domainChip.getStyleClass().addAll("chip", "chip-info");
        StackPane.setAlignment(domainChip, Pos.TOP_RIGHT);
        StackPane.setMargin(domainChip, new Insets(10));

        // Chip niveau
        Label niveauChip = new Label(safe(cours.getNiveau()));
        niveauChip.getStyleClass().addAll("chip", "chip-warning");
        StackPane.setAlignment(niveauChip, Pos.TOP_LEFT);
        StackPane.setMargin(niveauChip, new Insets(10));

        banner.getChildren().add(iv);
        if (!domainChip.getText().isBlank()) banner.getChildren().add(domainChip);
        if (!niveauChip.getText().isBlank()) banner.getChildren().add(niveauChip);

        // ── Corps ──
        VBox body = new VBox(8);
        body.setPadding(new Insets(14, 16, 14, 16));

        Label title = new Label(safe(cours.getTitre()));
        title.getStyleClass().add("project-card-title");
        title.setWrapText(true);

        String d = safe(cours.getDescription());
        Label desc = new Label(d.length() > 80 ? d.substring(0, 78) + "…" : d);
        desc.getStyleClass().add("project-card-subtitle");
        desc.setWrapText(true);

        // ── Footer ──
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -edu-border transparent transparent transparent; -fx-border-width: 1 0 0 0; -fx-padding: 8 0 0 0;");

        Label formateur = new Label("👤 " + (safe(cours.getNomFormateur()).isBlank() ? "N/A" : safe(cours.getNomFormateur())));
        formateur.getStyleClass().add("project-card-meta");
        formateur.setStyle("-fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label duree = new Label("⏱ " + cours.getDureeTotaleHeures() + "h");
        duree.getStyleClass().add("project-card-meta");
        duree.setStyle("-fx-font-size: 11px;");

        Label chapitres = new Label("📚 " + cours.getChapitreCount());
        chapitres.getStyleClass().add("project-card-meta");
        chapitres.setStyle("-fx-font-size: 11px;");
        Tooltip.install(chapitres, new Tooltip("Chapitres"));

        footer.getChildren().addAll(formateur, spacer, duree, chapitres);
        body.getChildren().addAll(title, desc, footer);
        card.getChildren().addAll(banner, body);
        return card;
    }

    private Image loadCourseImage(Cours cours) {
        String imgPath = safe(cours.getImage());
        if (!imgPath.isBlank() && !imgPath.startsWith("auto:")) {
            File f = new File(imgPath);
            if (f.exists()) {
                try { return new Image(f.toURI().toString(), 270, 150, false, true); }
                catch (Exception ignored) {}
            }
            if (imgPath.startsWith("http://") || imgPath.startsWith("https://")) {
                try { return new Image(imgPath, 270, 150, false, true, true); }
                catch (Exception ignored) {}
            }
            var res = getClass().getResource(imgPath.startsWith("/") ? imgPath : "/" + imgPath);
            if (res != null) {
                try { return new Image(res.toExternalForm(), 270, 150, false, true); }
                catch (Exception ignored) {}
            }
        }
        String domain = safe(cours.getDomaine()).toLowerCase();
        String path;
        if (domain.contains("informatique") || domain.contains("développement") || domain.contains("ia") || domain.contains("intelligence") || domain.contains("web") || domain.contains("mobile")) {
            path = "/assets/images/02.9bb56457.png";
        } else if (domain.contains("math") || domain.contains("physique") || domain.contains("data") || domain.contains("science")) {
            path = "/assets/images/ss.png";
        } else {
            path = "/assets/images/app-icon.png";
        }
        var url = getClass().getResource(path);
        return new Image(url == null ? "" : url.toExternalForm(), 270, 150, false, true);
    }

    private void openDetail(Cours cours) {
        try {
            javafx.fxml.FXMLLoader loader = Navigator.loader("View/front/FrontCourseDetail.fxml");
            javafx.scene.Parent root = loader.load();
            FrontCourseDetailController ctrl = loader.getController();
            ctrl.setCours(cours);
            // Naviguer dans le contentWrap du shell (pas remplacer tout le shell)
            javafx.scene.Node current = cardsFlow;
            while (current != null && !(current instanceof javafx.scene.layout.StackPane sp && sp.getId() != null && sp.getId().equals("contentWrap"))) {
                current = current.getParent();
            }
            if (current instanceof javafx.scene.layout.StackPane contentWrap) {
                contentWrap.getChildren().setAll(root);
            } else {
                // fallback : remplacer la scène
                javafx.scene.Scene scene = cardsFlow.getScene();
                if (scene != null) scene.setRoot(root);
            }
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

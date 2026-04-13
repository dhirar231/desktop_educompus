package com.educompus.controller.front;

import com.educompus.model.Cours;
import com.educompus.repository.CourseManagementRepository;
import com.educompus.nav.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class FrontCoursesController {
    private final CourseManagementRepository repository = new CourseManagementRepository();

    @FXML private VBox cardsBox;
    @FXML private Label totalCoursesLabel;

    @FXML
    private void initialize() {
        var courses = repository.listCours("");
        if (totalCoursesLabel != null) {
            totalCoursesLabel.setText(String.valueOf(courses.size()));
        }
        if (cardsBox == null) return;
        cardsBox.getChildren().clear();
        if (courses.isEmpty()) {
            Label empty = new Label("Aucun cours disponible pour le moment.");
            empty.getStyleClass().add("empty-title");
            cardsBox.getChildren().add(empty);
            return;
        }
        for (Cours cours : courses) {
            cardsBox.getChildren().add(buildCard(cours));
        }
    }

    private VBox buildCard(Cours cours) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("card", "front-course-card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-cursor: hand;");
        card.setOnMouseClicked(event -> openDetail(cours));

        StackPane media = new StackPane();
        media.getStyleClass().add("front-course-media");
        ImageView imageView = new ImageView(loadAutoImage(cours));
        imageView.setFitWidth(120);
        imageView.setFitHeight(90);
        imageView.setPreserveRatio(true);
        Label domainChip = new Label(safe(cours.getDomaine()));
        domainChip.getStyleClass().addAll("chip", "chip-info");
        StackPane.setAlignment(domainChip, Pos.TOP_RIGHT);
        StackPane.setMargin(domainChip, new Insets(10));
        media.getChildren().addAll(imageView, domainChip);

        Label title = new Label(safe(cours.getTitre()));
        title.getStyleClass().add("project-card-title");
        title.setWrapText(true);

        Label desc = new Label(safe(cours.getDescription()));
        desc.getStyleClass().add("project-card-subtitle");
        desc.setWrapText(true);

        Label level = new Label("Niveau: " + safe(cours.getNiveau()));
        level.getStyleClass().add("stat-title");
        Label duration = new Label("Durée: " + cours.getDureeTotaleHeures() + "h");
        duration.getStyleClass().add("stat-title");

        Label teacher = new Label("Formateur: " + safe(cours.getNomFormateur()));
        teacher.getStyleClass().add("stat-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label chapCount = new Label("Chapitres: " + cours.getChapitreCount());
        chapCount.getStyleClass().add("stat-title");

        HBox meta = new HBox(14, level, duration);
        HBox foot = new HBox(10, teacher, spacer, chapCount);
        foot.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(media, title, desc, meta, foot);
        return card;
    }

    private void openDetail(Cours cours) {
        try {
            javafx.fxml.FXMLLoader loader = Navigator.loader("View/front/FrontCourseDetail.fxml");
            javafx.scene.Parent root = loader.load();
            FrontCourseDetailController ctrl = loader.getController();
            ctrl.setCours(cours);
            javafx.scene.Scene scene = cardsBox.getScene();
            if (scene != null) scene.setRoot(root);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private Image loadAutoImage(Cours cours) {
        String domain = safe(cours.getDomaine()).toLowerCase();
        String path;
        if (domain.contains("informatique")) {
            path = "/assets/images/02.9bb56457.png";
        } else if (domain.contains("mathematiques") || domain.contains("physique")) {
            path = "/assets/images/ss.png";
        } else {
            path = "/assets/images/app-icon.png";
        }
        var url = getClass().getResource(path);
        return new Image(url == null ? "" : url.toExternalForm(), true);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

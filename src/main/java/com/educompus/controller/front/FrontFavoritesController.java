package com.educompus.controller.front;

import com.educompus.app.AppState;
import com.educompus.model.Cours;
import com.educompus.nav.Navigator;
import com.educompus.repository.CourseFavoriteRepository;
import com.educompus.repository.CourseManagementRepository;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FrontFavoritesController {

    private final CourseFavoriteRepository favRepo   = new CourseFavoriteRepository();
    private final CourseManagementRepository courseRepo = new CourseManagementRepository();

    @FXML private FlowPane cardsFlow;
    @FXML private Label    totalLabel;
    @FXML private Label    lblCount;
    @FXML private TextField searchField;
    @FXML private VBox     emptyState;

    private List<Cours> allFavorites = new ArrayList<>();
    private int studentId;

    @FXML
    private void initialize() {
        studentId = AppState.getUserId();
        loadFavorites();
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyFilter(n));
        }
    }

    // ── Chargement ──────────────────────────────────────────────────────────────

    private void loadFavorites() {
        allFavorites.clear();
        try {
            Set<Integer> ids = favRepo.listFavoriteCourseIds(studentId);
            List<Cours> all  = courseRepo.listCours("");
            for (Cours c : all) {
                if (ids.contains(c.getId())) {
                    allFavorites.add(c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (totalLabel != null) totalLabel.setText(String.valueOf(allFavorites.size()));
        renderCards(allFavorites);
    }

    @FXML
    private void onReset() {
        if (searchField != null) searchField.clear();
        renderCards(allFavorites);
    }

    private void applyFilter(String query) {
        if (query == null || query.isBlank()) {
            renderCards(allFavorites);
            return;
        }
        String q = query.trim().toLowerCase();
        List<Cours> filtered = allFavorites.stream()
            .filter(c -> safe(c.getTitre()).toLowerCase().contains(q)
                      || safe(c.getDomaine()).toLowerCase().contains(q)
                      || safe(c.getNomFormateur()).toLowerCase().contains(q))
            .toList();
        renderCards(filtered);
    }

    // ── Rendu cartes ─────────────────────────────────────────────────────────────

    private void renderCards(List<Cours> courses) {
        if (cardsFlow == null) return;
        cardsFlow.getChildren().clear();
        boolean empty = courses.isEmpty();
        if (emptyState != null) { emptyState.setVisible(empty); emptyState.setManaged(empty); }
        if (lblCount != null) lblCount.setText(empty ? "" : courses.size() + " résultat" + (courses.size() > 1 ? "s" : ""));
        for (Cours cours : courses) cardsFlow.getChildren().add(buildCard(cours));
    }

    private VBox buildCard(Cours cours) {
        VBox card = new VBox(0);
        card.getStyleClass().add("project-card");
        card.setPrefWidth(270);
        card.setMaxWidth(270);
        card.setStyle("-fx-cursor: hand;");

        // ── Bannière ─────────────────────────────────────────────────────────
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

        Label domainChip = new Label(safe(cours.getDomaine()).isBlank() ? "Général" : safe(cours.getDomaine()));
        domainChip.getStyleClass().addAll("chip", "chip-info");
        StackPane.setAlignment(domainChip, Pos.TOP_RIGHT);
        StackPane.setMargin(domainChip, new Insets(10));

        Label niveauChip = new Label(safe(cours.getNiveau()));
        niveauChip.getStyleClass().addAll("chip", "chip-warning");
        StackPane.setAlignment(niveauChip, Pos.TOP_LEFT);
        StackPane.setMargin(niveauChip, new Insets(10));

        // Bouton retirer des favoris (❤ plein)
        StackPane favBtn = buildFavBtn(cours, true, card);
        StackPane.setAlignment(favBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(favBtn, new Insets(0, 10, 10, 0));

        banner.getChildren().add(iv);
        if (!domainChip.getText().isBlank()) banner.getChildren().add(domainChip);
        if (!niveauChip.getText().isBlank()) banner.getChildren().add(niveauChip);
        banner.getChildren().add(favBtn);

        // ── Corps ────────────────────────────────────────────────────────────
        VBox body = new VBox(8);
        body.setPadding(new Insets(14, 16, 14, 16));

        Label title = new Label(safe(cours.getTitre()));
        title.getStyleClass().add("project-card-title");
        title.setWrapText(true);

        String d = safe(cours.getDescription());
        Label desc = new Label(d.length() > 80 ? d.substring(0, 78) + "…" : d);
        desc.getStyleClass().add("project-card-subtitle");
        desc.setWrapText(true);

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

        // Clic carte → détail ; le bouton cœur consomme lui-même l'événement (e.consume() dans buildFavBtn)
        card.setOnMouseClicked(e -> openDetail(cours));

        card.getChildren().addAll(banner, body);
        return card;
    }

    // ── Bouton cœur ──────────────────────────────────────────────────────────────

    private StackPane buildFavBtn(Cours cours, boolean isFav, VBox card) {
        SVGPath heart = new SVGPath();
        // Cœur plein ou vide
        heart.setContent("M12 21.35L10.55 20.03C5.4 15.36 2 12.27 2 8.5C2 5.41 4.42 3 7.5 3C9.24 3 10.91 3.81 12 5.08C13.09 3.81 14.76 3 16.5 3C19.58 3 22 5.41 22 8.5C22 12.27 18.6 15.36 13.45 20.03L12 21.35Z");
        heart.getStyleClass().add(isFav ? "fav-btn-filled" : "fav-btn-outline");

        StackPane btn = new StackPane(heart);
        btn.getStyleClass().add("fav-btn");
        btn.setMinSize(32, 32);
        btn.setPrefSize(32, 32);
        btn.setMaxSize(32, 32);
        Tooltip.install(btn, new Tooltip(isFav ? "Retirer des favoris" : "Ajouter aux favoris"));

        btn.setOnMouseClicked(e -> {
            e.consume();
            toggleFavorite(cours, heart, btn, card);
        });
        return btn;
    }

    private void toggleFavorite(Cours cours, SVGPath heart, StackPane btn, VBox card) {
        // Animation de rebond
        ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.35);  st.setToY(1.35);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();

        try {
            // Retirer le favori
            favRepo.removeFavorite(studentId, cours.getId());
            allFavorites.remove(cours);
            if (totalLabel != null) totalLabel.setText(String.valueOf(allFavorites.size()));
            // Retirer la carte avec animation
            st.setOnFinished(ev -> {
                cardsFlow.getChildren().remove(card);
                boolean empty = cardsFlow.getChildren().isEmpty();
                if (emptyState != null) { emptyState.setVisible(empty); emptyState.setManaged(empty); }
                if (lblCount != null) lblCount.setText(empty ? "" : cardsFlow.getChildren().size() + " résultat(s)");
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ── Navigation détail ────────────────────────────────────────────────────────

    private void openDetail(Cours cours) {
        try {
            javafx.fxml.FXMLLoader loader = Navigator.loader("View/front/FrontCourseDetail.fxml");
            javafx.scene.Parent root = loader.load();
            FrontCourseDetailController ctrl = loader.getController();
            ctrl.setCours(cours);
            javafx.scene.Node current = cardsFlow;
            while (current != null) {
                if (current instanceof StackPane sp && "contentWrap".equals(sp.getId())) {
                    sp.getChildren().setAll(root);
                    return;
                }
                current = current.getParent();
            }
            javafx.scene.Scene scene = cardsFlow.getScene();
            if (scene != null) scene.setRoot(root);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            e.printStackTrace();
            alert.showAndWait();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private Image loadCourseImage(Cours cours) {
        String imgPath = safe(cours.getImage());
        if (!imgPath.isBlank() && !imgPath.startsWith("auto:")) {
            File f = new File(imgPath);
            if (f.exists()) { try { return new Image(f.toURI().toString(), 270, 150, false, true); } catch (Exception ignored) {} }
            if (imgPath.startsWith("http://") || imgPath.startsWith("https://")) { try { return new Image(imgPath, 270, 150, false, true, true); } catch (Exception ignored) {} }
            var res = getClass().getResource(imgPath.startsWith("/") ? imgPath : "/" + imgPath);
            if (res != null) { try { return new Image(res.toExternalForm(), 270, 150, false, true); } catch (Exception ignored) {} }
        }
        String domain = safe(cours.getDomaine()).toLowerCase();
        String path = (domain.contains("informatique") || domain.contains("développement") || domain.contains("ia") || domain.contains("web") || domain.contains("mobile"))
            ? "/assets/images/02.9bb56457.png"
            : (domain.contains("math") || domain.contains("physique") || domain.contains("data") || domain.contains("science"))
            ? "/assets/images/ss.png"
            : "/assets/images/app-icon.png";
        var url = getClass().getResource(path);
        return new Image(url == null ? "" : url.toExternalForm(), 270, 150, false, true);
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }
}

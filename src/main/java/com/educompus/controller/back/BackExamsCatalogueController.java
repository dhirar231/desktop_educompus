package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.model.ExamCatalogueItem;
import com.educompus.nav.Navigator;
import com.educompus.repository.ExamRepository;
import com.educompus.util.Theme;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BackExamsCatalogueController {
    @FXML private FlowPane catalogueFlow;
    @FXML private StackPane contentPane;
    @FXML private javafx.scene.control.ScrollPane cardsScroll;
    @FXML private javafx.scene.control.MenuButton viewToggle;
    @FXML private javafx.scene.control.MenuItem viewResponsesItem;
    @FXML private javafx.scene.control.TextField searchField;
    @FXML private javafx.scene.control.ComboBox<String> sortCombo;
    @FXML private HBox tableControlsBar;

    private final ExamRepository repository = new ExamRepository();
    private final List<ExamCatalogueItem> catalogue = new ArrayList<>();
    private BackExamsController embeddedTableController;
    private long lastSeenChangeCounter = Long.MIN_VALUE;

    @FXML
    private void initialize() {
        if (sortCombo != null) {
            sortCombo.getItems().setAll("Cours A-Z", "Cours Z-A", "Questions desc");
            sortCombo.setValue("Cours A-Z");
            sortCombo.valueProperty().addListener((obs, oldValue, newValue) -> applySortAndRender());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> reloadCatalogue());
        }

        reloadCatalogue();
        if (cardsScroll != null && catalogueFlow != null) {
            catalogueFlow.prefWrapLengthProperty().bind(cardsScroll.widthProperty().subtract(80));
        }
        showCards();
        observeExamChanges();

        boolean allowed = AppState.isAdmin() || AppState.isTeacher();
        if (viewResponsesItem != null) {
            viewResponsesItem.setVisible(allowed);
        }
    }

    private void observeExamChanges() {
        lastSeenChangeCounter = ExamRepository.CHANGE_COUNTER.get();
        ExamRepository.CHANGE_COUNTER.addListener((obs, oldValue, newValue) -> {
            long next = newValue == null ? Long.MIN_VALUE : newValue.longValue();
            if (next == lastSeenChangeCounter) {
                return;
            }
            lastSeenChangeCounter = next;
            Platform.runLater(this::reloadCatalogue);
        });
    }

    @FXML
    private void reloadCatalogue() {
        String query = searchField == null ? "" : searchField.getText();
        catalogue.clear();
        catalogue.addAll(repository.listAdminRows(query == null ? "" : query));
        applySortAndRender();
    }

    private void applySortAndRender() {
        String sort = sortCombo == null ? "Cours A-Z" : safe(sortCombo.getValue());
        if ("Cours Z-A".equalsIgnoreCase(sort)) {
            catalogue.sort(Comparator.comparing((ExamCatalogueItem item) -> safe(item.getCourseTitle()), String.CASE_INSENSITIVE_ORDER).reversed());
        } else if ("Questions desc".equalsIgnoreCase(sort)) {
            catalogue.sort(Comparator.comparingInt(ExamCatalogueItem::getQuestionCount).reversed());
        } else {
            catalogue.sort(Comparator.comparing((ExamCatalogueItem item) -> safe(item.getCourseTitle()), String.CASE_INSENSITIVE_ORDER));
        }
        renderCatalogue();
    }

    private void renderCatalogue() {
        if (catalogueFlow == null) return;
        catalogueFlow.getChildren().clear();
        for (ExamCatalogueItem item : catalogue) {
            catalogueFlow.getChildren().add(buildCatalogueCard(item));
        }
    }

    private VBox buildCatalogueCard(ExamCatalogueItem item) {
        Label courseTitle = new Label(item.getCourseTitle());
        courseTitle.getStyleClass().add("exam-card-title");
        courseTitle.setWrapText(true);

        Button viewBtn = new Button("Voir réponses");
        viewBtn.getStyleClass().add("btn-rgb-outline");
        viewBtn.setOnAction(event -> openResponsesForExam(item));

        Button publishBtn = new Button(item.isPublished() ? "Dépublier" : "Publier");
        publishBtn.getStyleClass().add(item.isPublished() ? "btn-rgb-outline" : "btn-rgb");
        publishBtn.setOnAction(event -> togglePublish(item));
        Tooltip.install(
                publishBtn,
                new Tooltip(item.isPublished()
                    ? "Dépublier rendra l'examen indisponible aux étudiants."
                    : "Publier rendra l'examen visible et accessible aux étudiants.")
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = AppState.isAdmin()
                ? new HBox(8, spacer, viewBtn, publishBtn)
                : new HBox(8, spacer, viewBtn);
        footer.getStyleClass().add("exam-card-footer");

        VBox card = new VBox(6, courseTitle, footer);
        card.getStyleClass().addAll("card", "exam-course-card", "exam-card-minimal");
        card.setPrefWidth(320);
        card.setMinHeight(120);
        return card;
    }

    private void togglePublish(ExamCatalogueItem item) {
        if (item == null) return;
        if (!AppState.isAdmin()) {
            info("Permission", "Seul l'administrateur peut publier/dépublier cet examen.");
            return;
        }

        String action = item.isPublished() ? "dépublier" : "publier";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText((item.isPublished() ? "Dépublier" : "Publier") + " l'examen ?");
        confirm.setContentText("Examen: " + safe(item.getExamTitle()) + "\nVoulez-vous vraiment " + action + " cet examen ?");
        styleDialog(confirm);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            repository.setPublished(item.getExamId(), !item.isPublished());
            item.setPublished(!item.isPublished());
            reloadCatalogue();
            String message = item.isPublished() ? "Examen publié." : "Examen dépublié.";
            info("Publication", message);
            showSnackbar(message);
        } catch (Exception e) {
            error("Erreur publication examen", e);
        }
    }

    private void showSnackbar(String message) {
        try {
            if (contentPane == null) return;
            Label snack = new Label(message);
            snack.getStyleClass().add("snackbar");
            snack.setOpacity(0);
            StackPane.setAlignment(snack, Pos.TOP_RIGHT);
            contentPane.getChildren().add(snack);

            FadeTransition in = new FadeTransition(Duration.millis(220), snack);
            in.setFromValue(0);
            in.setToValue(1);
            PauseTransition wait = new PauseTransition(Duration.seconds(2.2));
            FadeTransition out = new FadeTransition(Duration.millis(300), snack);
            out.setFromValue(1);
            out.setToValue(0);
            out.setOnFinished(event -> contentPane.getChildren().remove(snack));

            in.play();
            in.setOnFinished(event -> wait.play());
            wait.setOnFinished(event -> out.play());
        } catch (Exception ignored) {
        }
    }

    private void openQuestions(ExamCatalogueItem item) {
        if (item == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/back/BackExamQuestions.fxml"));
            Parent root = loader.load();
            BackExamQuestionsController controller = loader.getController();
            if (controller != null) controller.setExam(item);

            Window owner = catalogueFlow == null || catalogueFlow.getScene() == null ? null : catalogueFlow.getScene().getWindow();
            Stage stage = new Stage();
            stage.setTitle("Questions & réponses - " + item.getExamTitle());
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) stage.initOwner(owner);

            Scene scene = new Scene(root, 1180, 760);
            if (catalogueFlow != null && catalogueFlow.getScene() != null) {
                scene.getStylesheets().setAll(catalogueFlow.getScene().getStylesheets());
            }
            Theme.apply(root);
            stage.setScene(scene);
            stage.showAndWait();
            reloadCatalogue();
        } catch (Exception e) {
            error("Erreur questions", e);
        }
    }

    private void openResponsesForExam(ExamCatalogueItem item) {
        if (item == null) return;
        try {
            FXMLLoader loader = Navigator.loader("View/back/TeacherResponses.fxml");
            Parent root = loader.load();
            TeacherResponsesController controller = loader.getController();
            if (controller != null) controller.setExamId(item.getExamId());

            Window owner = catalogueFlow == null || catalogueFlow.getScene() == null ? null : catalogueFlow.getScene().getWindow();
            Stage stage = new Stage();
            stage.setTitle("Réponses - " + item.getExamTitle());
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) stage.initOwner(owner);

            Scene scene = new Scene(root, 980, 640);
            if (catalogueFlow != null && catalogueFlow.getScene() != null) {
                scene.getStylesheets().setAll(catalogueFlow.getScene().getStylesheets());
            }
            Theme.apply(root);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            error("Erreur réponses", e);
        }
    }

    @FXML
    private void showTable() {
        try {
            FXMLLoader loader = Navigator.loader("View/back/BackExams.fxml");
            Parent root = loader.load();
            embeddedTableController = loader.getController();

            if (contentPane != null) {
                Node tablePane = root.lookup("#examsTablePane");
                contentPane.getChildren().setAll(tablePane != null ? tablePane : root);
            }
            if (viewToggle != null) viewToggle.setText("Vue Table");
            if (tableControlsBar != null) {
                tableControlsBar.setVisible(true);
                tableControlsBar.setManaged(true);
            }
        } catch (Exception e) {
            error("Erreur affichage table", e);
        }
    }

    @FXML
    private void showCards() {
        if (contentPane != null && cardsScroll != null) {
            contentPane.getChildren().setAll(cardsScroll);
        }
        if (viewToggle != null) viewToggle.setText("Vue Cartes");
        if (tableControlsBar != null) {
            tableControlsBar.setVisible(false);
            tableControlsBar.setManaged(false);
        }
    }

    @FXML
    private void showResponses() {
        if (!(AppState.isAdmin() || AppState.isTeacher())) {
            info("Accès refusé", "Vous n'êtes pas autorisé à voir les réponses.");
            return;
        }
        try {
            FXMLLoader loader = Navigator.loader("View/back/TeacherResponses.fxml");
            Parent view = loader.load();
            if (contentPane != null) {
                contentPane.getChildren().setAll(view);
            }
            if (viewToggle != null) viewToggle.setText("Voir réponses");
            if (tableControlsBar != null) {
                tableControlsBar.setVisible(false);
                tableControlsBar.setManaged(false);
            }
        } catch (Exception e) {
            error("Erreur affichage reponses", e);
        }
    }

    @FXML
    private void newExam(ActionEvent event) {
        ensureTableMode();
        if (embeddedTableController != null) embeddedTableController.triggerNewExam();
    }

    @FXML
    private void editExam(ActionEvent event) {
        ensureTableMode();
        if (embeddedTableController != null) embeddedTableController.triggerEditExam();
    }

    @FXML
    private void saveExam(ActionEvent event) {
        newExam(event);
    }

    @FXML
    private void deleteExam(ActionEvent event) {
        ensureTableMode();
        if (embeddedTableController != null) {
            embeddedTableController.triggerDeleteExam();
            reloadCatalogue();
        }
    }

    @FXML
    private void openQuestionsForSelectedExam(ActionEvent event) {
        ensureTableMode();
        if (embeddedTableController != null) {
            embeddedTableController.triggerOpenQuestionsPage();
            reloadCatalogue();
        }
    }

    private void ensureTableMode() {
        if (embeddedTableController == null) {
            showTable();
        }
    }

    private static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleDialog(alert);
        alert.showAndWait();
    }

    private static void error(String title, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(e == null ? "Erreur" : String.valueOf(e.getMessage()));
        styleDialog(alert);
        alert.showAndWait();
        if (e != null) e.printStackTrace();
    }

    private static void styleDialog(javafx.scene.control.Dialog<?> dialog) {
        try { com.educompus.util.Dialogs.style(dialog); } catch (Exception ignored) {}
    }

    private static String cssUri() {
        java.io.File file = new java.io.File("styles/educompus.css");
        if (!file.exists()) file = new java.io.File("eduCompus-javafx/styles/educompus.css");
        if (!file.exists()) file = new java.io.File(new java.io.File("..", "eduCompus-javafx"), "styles/educompus.css");
        return file.exists() ? file.toURI().toString() : "";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

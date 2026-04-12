package com.educompus.controller.back;

import com.educompus.model.ExamCatalogueItem;
import com.educompus.repository.ExamRepository;
import com.educompus.app.AppState;
import com.educompus.nav.Navigator;
import com.educompus.util.Theme;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Tooltip;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;

public final class BackExamsCatalogueController {
    @FXML
    private FlowPane catalogueFlow;
    @FXML
    private StackPane contentPane;
    @FXML
    private javafx.scene.control.ScrollPane cardsScroll;
    @FXML
    private javafx.scene.control.MenuButton viewToggle;
    @FXML
    private javafx.scene.control.TextField searchField;
    @FXML
    private javafx.scene.control.ComboBox<String> sortCombo;
    @FXML
    private HBox tableControlsBar;

    private final ExamRepository repository = new ExamRepository();
    private final List<ExamCatalogueItem> catalogue = new ArrayList<>();

    @FXML
    private void initialize() {
        // setup search + sort
        if (sortCombo != null) {
            sortCombo.getItems().setAll("Cours A-Z", "Cours Z-A", "Questions desc");
            sortCombo.setValue("Cours A-Z");
            sortCombo.valueProperty().addListener((obs, oldV, newV) -> applySortAndRender());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> reloadCatalogue());
        }

        reloadCatalogue();
        // ensure the cards scroll pane is used and wrap length adapts to its width
        if (cardsScroll != null && catalogueFlow != null) {
            catalogueFlow.prefWrapLengthProperty().bind(cardsScroll.widthProperty().subtract(80));
        }
        // show cards by default
        showCards();
    }

    @FXML
    private void reloadCatalogue() {
        String q = searchField == null ? "" : searchField.getText();
        catalogue.clear();
        catalogue.addAll(repository.listAdminRows(q == null ? "" : q));
        // initial sort will be handled by applySortAndRender to respect the selected sort option
        applySortAndRender();
    }

    private void renderCatalogue() {
        if (catalogueFlow == null) return;
        catalogueFlow.getChildren().clear();
        for (ExamCatalogueItem item : catalogue) {
            VBox card = buildCatalogueCard(item);
            catalogueFlow.getChildren().add(card);
        }
        // no direct manipulation of contentPane here; showCards()/showTable() handle swapping
    }

    private void applySortAndRender() {
        String sort = sortCombo == null ? "Cours A-Z" : String.valueOf(sortCombo.getValue());
        if ("Cours Z-A".equalsIgnoreCase(sort)) {
            catalogue.sort(Comparator.comparing((ExamCatalogueItem item) -> safe(item.getCourseTitle()), String.CASE_INSENSITIVE_ORDER).reversed());
        } else if ("Questions desc".equalsIgnoreCase(sort)) {
            catalogue.sort(Comparator.comparingInt(ExamCatalogueItem::getQuestionCount).reversed());
        } else {
            catalogue.sort(Comparator.comparing((ExamCatalogueItem item) -> safe(item.getCourseTitle()), String.CASE_INSENSITIVE_ORDER));
        }
        renderCatalogue();
    }

    private VBox buildCatalogueCard(ExamCatalogueItem item) {
        // Minimal professional card: course title + actions
        Label courseTitle = new Label(item.getCourseTitle());
        courseTitle.getStyleClass().add("exam-card-title");
        courseTitle.setWrapText(true);

        Button viewBtn = new Button("Voir");
        viewBtn.getStyleClass().add("btn-rgb-outline");
        viewBtn.setOnAction(ev -> openQuestions(item));

        Button pubBtn = new Button(item.isPublished() ? "Depublier" : "Publier");
        pubBtn.getStyleClass().addAll(item.isPublished() ? "btn-rgb-outline" : "btn-rgb");
        pubBtn.setOnAction(ev -> togglePublish(item));
        Tooltip tt = new Tooltip(item.isPublished() ? "Depublier rendra l'examen indisponible aux etudiants." : "Publier rendra l'examen visible et accessible aux etudiants.");
        Tooltip.install(pubBtn, tt);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer;
        if (AppState.isAdmin()) {
            footer = new HBox(8, spacer, viewBtn, pubBtn);
        } else {
            footer = new HBox(8, spacer, viewBtn);
        }
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
            info("Permission", "Seul l'administrateur peut publier/depublier cet examen.");
            return;
        }
        String action = item.isPublished() ? "depublier" : "publier";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText((item.isPublished() ? "Depublier" : "Publier") + " l'examen ?");
        confirm.setContentText("Examen: " + safe(item.getExamTitle()) + "\nVoulez-vous vraiment " + action + " cet examen ?");
        styleDialog(confirm);
        if (confirm.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) != javafx.scene.control.ButtonType.OK) return;

        try {
            repository.setPublished(item.getExamId(), !item.isPublished());
            item.setPublished(!item.isPublished());
            reloadCatalogue();
            String msg = item.isPublished() ? "Examen publie." : "Examen depublie.";
            info("Publication", msg);
            showSnackbar(msg);
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
            // position top-right
            StackPane.setAlignment(snack, Pos.TOP_RIGHT);
            contentPane.getChildren().add(snack);

            FadeTransition in = new FadeTransition(Duration.millis(220), snack);
            in.setFromValue(0);
            in.setToValue(1);

            PauseTransition wait = new PauseTransition(Duration.seconds(2.2));

            FadeTransition out = new FadeTransition(Duration.millis(300), snack);
            out.setFromValue(1);
            out.setToValue(0);
            out.setOnFinished(ev -> contentPane.getChildren().remove(snack));

            in.play();
            in.setOnFinished(ev -> wait.play());
            wait.setOnFinished(ev -> out.play());
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
            stage.setTitle("Questions & reponses - " + item.getExamTitle());
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

    @FXML
    private void showTable() {
        try {
            // load the existing table view and display it inside the content pane
            FXMLLoader loader = Navigator.loader("View/back/BackExams.fxml");
            Parent table = loader.load();
            if (contentPane != null) {
                // try to extract the SplitPane content to avoid duplicating the page header
                Node split = table.lookup("#examsSplit");
                if (split != null) {
                    contentPane.getChildren().setAll(split);
                } else {
                    contentPane.getChildren().setAll(table);
                }
                if (viewToggle != null) viewToggle.setText("Vue Table");
                if (tableControlsBar != null) {
                    tableControlsBar.setVisible(true);
                    tableControlsBar.setManaged(true);
                }
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

    // --- CRUD proxy actions when table is embedded in the catalogue ---
    @FXML
    private void newExam(ActionEvent ev) {
        try {
            Node split = contentPane == null || contentPane.getChildren().isEmpty() ? null : contentPane.getChildren().get(0);
            if (split == null) {
                // nothing to operate on
                info("Examen", "Aucun tableau actif.");
                return;
            }
            TableView<?> table = (TableView<?>) split.lookup("#examsTable");
            if (table != null) {
                table.getSelectionModel().clearSelection();
            }
            // clear form fields if present
            javafx.scene.control.TextField title = (javafx.scene.control.TextField) split.lookup("#examTitleField");
            javafx.scene.control.TextField course = (javafx.scene.control.TextField) split.lookup("#courseIdField");
            javafx.scene.control.TextField level = (javafx.scene.control.TextField) split.lookup("#levelField");
            javafx.scene.control.TextField domain = (javafx.scene.control.TextField) split.lookup("#domainField");
            javafx.scene.control.TextArea desc = (javafx.scene.control.TextArea) split.lookup("#examDescriptionArea");
            if (title != null) title.setText("");
            if (course != null) course.setText("");
            if (level != null) level.setText("");
            if (domain != null) domain.setText("");
            if (desc != null) desc.setText("");
        } catch (Exception e) {
            error("Examen", e);
        }
    }

    @FXML
    private void editExam(ActionEvent ev) {
        try {
            Node split = contentPane == null || contentPane.getChildren().isEmpty() ? null : contentPane.getChildren().get(0);
            if (split == null) {
                info("Examen", "Aucun tableau actif.");
                return;
            }
            TableView<?> table = (TableView<?>) split.lookup("#examsTable");
            if (table == null || table.getSelectionModel().getSelectedItem() == null) {
                info("Examen", "Selectionnez un examen a modifier.");
                return;
            }
            // selection is enough — BackExamsController would populate fields; here we just ensure focus
            javafx.scene.control.TextField title = (javafx.scene.control.TextField) split.lookup("#examTitleField");
            if (title != null) {
                title.requestFocus();
                title.selectAll();
            }
        } catch (Exception e) {
            error("Examen", e);
        }
    }

    @FXML
    private void saveExam(ActionEvent ev) {
        try {
            Node split = contentPane == null || contentPane.getChildren().isEmpty() ? null : contentPane.getChildren().get(0);
            if (split == null) {
                info("Examen", "Aucun tableau actif.");
                return;
            }

            javafx.scene.control.TextField title = (javafx.scene.control.TextField) split.lookup("#examTitleField");
            javafx.scene.control.TextField course = (javafx.scene.control.TextField) split.lookup("#courseIdField");
            javafx.scene.control.TextField level = (javafx.scene.control.TextField) split.lookup("#levelField");
            javafx.scene.control.TextField domain = (javafx.scene.control.TextField) split.lookup("#domainField");
            javafx.scene.control.TextArea desc = (javafx.scene.control.TextArea) split.lookup("#examDescriptionArea");
            TableView<?> table = (TableView<?>) split.lookup("#examsTable");

            String t = title == null ? "" : safe(title.getText());
            String d = desc == null ? "" : safe(desc.getText());
            String lev = level == null ? "" : safe(level.getText());
            String dom = domain == null ? "" : safe(domain.getText());

            if (t.isBlank()) {
                info("Examen", "Le titre de l'examen est obligatoire.");
                return;
            }
            if (t.length() > 200) {
                info("Examen", "Le titre ne doit pas depasser 200 caracteres.");
                return;
            }
            if (d.length() > 2000) {
                info("Examen", "La description est trop longue (max 2000 caracteres).");
                return;
            }

            int cid;
            try {
                cid = Integer.parseInt(course == null ? "0" : safe(course.getText()));
                if (cid <= 0) {
                    info("Examen", "Le champ Cours ID doit etre un entier positif.");
                    return;
                }
            } catch (Exception ex) {
                info("Examen", "Le champ Cours ID doit etre un entier valide.");
                return;
            }

            // Determine if editing existing (selected row) or new
            Integer existingId = null;
            if (table != null && table.getSelectionModel().getSelectedItem() != null) {
                Object sel = table.getSelectionModel().getSelectedItem();
                try {
                    java.lang.reflect.Method m = sel.getClass().getMethod("getExamId");
                    Object idv = m.invoke(sel);
                    if (idv instanceof Number) existingId = ((Number) idv).intValue();
                } catch (Exception ignored) {}
            }

            com.educompus.model.ExamCatalogueItem item = new com.educompus.model.ExamCatalogueItem();
            if (existingId != null) item.setExamId(existingId);
            item.setExamTitle(t);
            item.setExamDescription(d);
            item.setLevelLabel(lev);
            item.setDomainLabel(dom);
            item.setCourseId(cid);

            if (existingId == null || existingId <= 0) {
                repository.addExam(item);
                info("Examen", "Examen ajoute.");
            } else {
                repository.updateExam(item);
                info("Examen", "Examen mis a jour.");
            }

            // refresh both table view and catalogue
            showTable();
            reloadCatalogue();
        } catch (Exception e) {
            error("Erreur examen", e);
        }
    }

    @FXML
    private void deleteExam(ActionEvent ev) {
        try {
            Node split = contentPane == null || contentPane.getChildren().isEmpty() ? null : contentPane.getChildren().get(0);
            if (split == null) {
                info("Examen", "Aucun tableau actif.");
                return;
            }
            TableView<?> table = (TableView<?>) split.lookup("#examsTable");
            if (table == null || table.getSelectionModel().getSelectedItem() == null) {
                info("Examen", "Selectionnez un examen a supprimer.");
                return;
            }
            Object sel = table.getSelectionModel().getSelectedItem();
            Integer id = null;
            try {
                java.lang.reflect.Method m = sel.getClass().getMethod("getExamId");
                Object idv = m.invoke(sel);
                if (idv instanceof Number) id = ((Number) idv).intValue();
            } catch (Exception ignored) {}
            if (id == null) {
                info("Examen", "Impossible de determiner l'examen selectionne.");
                return;
            }

            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Supprimer examen");
            confirm.setHeaderText("Confirmer la suppression");
            confirm.setContentText("Examen ID: " + id);
            styleDialog(confirm);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

            repository.deleteExam(id);
            info("Examen", "Examen supprime.");
            showTable();
            reloadCatalogue();
        } catch (Exception e) {
            error("Erreur suppression examen", e);
        }
    }

    // --- small helpers copied from other controllers ---
    private static Label chip(String text, String styles) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styles.split(" "));
        return label;
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
        if (dialog == null || dialog.getDialogPane() == null) return;
        String css = cssUri();
        if (!css.isBlank() && !dialog.getDialogPane().getStylesheets().contains(css)) {
            dialog.getDialogPane().getStylesheets().add(css);
        }
        if (!dialog.getDialogPane().getStyleClass().contains("rgb-dialog")) {
            dialog.getDialogPane().getStyleClass().add("rgb-dialog");
        }
    }

    private static String cssUri() {
        java.io.File file = new java.io.File("styles/educompus.css");
        if (!file.exists()) file = new java.io.File("eduCompus-javafx/styles/educompus.css");
        if (!file.exists()) file = new java.io.File(new java.io.File("..", "eduCompus-javafx"), "styles/educompus.css");
        return file.exists() ? file.toURI().toString() : "";
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }

    private static String summarize(String value, int max) {
        String clean = safe(value).replace('\n', ' ').replace('\r', ' ');
        if (clean.length() <= max) return clean;
        return clean.substring(0, Math.max(0, max - 1)).trim() + "...";
    }
}

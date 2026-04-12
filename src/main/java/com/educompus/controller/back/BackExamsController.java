package com.educompus.controller.back;

import com.educompus.model.ExamCatalogueItem;
import com.educompus.repository.ExamRepository;
import com.educompus.util.Theme;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BackExamsController {
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> examSortCombo;
    @FXML
    private Label totalExamsLabel;
    @FXML
    private Label totalCoursesLabel;
    @FXML
    private Label readyQuizzesLabel;
    @FXML
    private TableView<ExamCatalogueItem> examsTable;
    @FXML
    private TableColumn<ExamCatalogueItem, Number> colExamId;
    @FXML
    private TableColumn<ExamCatalogueItem, String> colExamTitle;
    @FXML
    private TableColumn<ExamCatalogueItem, String> colCourse;
    @FXML
    private TableColumn<ExamCatalogueItem, String> colLevel;
    @FXML
    private TableColumn<ExamCatalogueItem, Number> colQuestions;
    @FXML
    private TableColumn<ExamCatalogueItem, String> colStatus;
    @FXML
    private TextField examTitleField;
    @FXML
    private TextField courseIdField;
    @FXML
    private TextField levelField;
    @FXML
    private TextField domainField;
    @FXML
    private TextArea examDescriptionArea;
    @FXML
    private Label selectedExamLabel;

    private final ExamRepository repository = new ExamRepository();
    private final ObservableList<ExamCatalogueItem> exams = FXCollections.observableArrayList();
    private ExamCatalogueItem editingExam;

    @FXML
    private void initialize() {
        setupTable();
        setupSort();
        reload();
    }

    private void setupTable() {
        examsTable.setItems(exams);
        colExamId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getExamId()));
        colExamTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getExamTitle()));
        colCourse.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourseTitle()));
        colLevel.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLevelLabel()));
        colQuestions.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getQuestionCount()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusLabel()));
        colExamId.setVisible(false);
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.getStyleClass().add("chip");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                badge.setText(item);
                badge.getStyleClass().removeAll("chip-success", "chip-warning");
                badge.getStyleClass().add("Pret".equalsIgnoreCase(item) ? "chip-success" : "chip-warning");
                setGraphic(badge);
                setText(null);
            }
        });

        examsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            editingExam = newValue;
            fillExamForm(newValue);
        });
    }

    private void setupSort() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> reload());
        }
        if (examSortCombo != null) {
            examSortCombo.getItems().setAll("Titre A-Z", "Titre Z-A", "Cours A-Z", "Questions desc");
            examSortCombo.setValue("Titre A-Z");
            examSortCombo.valueProperty().addListener((obs, oldValue, newValue) -> applySort());
        }
    }

    @FXML
    private void reload() {
        List<ExamCatalogueItem> items = repository.listAdminRows(text(searchField));
        exams.setAll(items);
        applySort();
        updateStats(items);

        int selectedId = editingExam == null ? 0 : editingExam.getExamId();
        if (selectedId > 0) {
            selectExamById(selectedId);
        }
        if (!exams.isEmpty() && examsTable.getSelectionModel().getSelectedItem() == null) {
            examsTable.getSelectionModel().selectFirst();
        }
        if (exams.isEmpty()) {
            fillExamForm(null);
        }
    }

    private void updateStats(List<ExamCatalogueItem> items) {
        Set<Integer> courseIds = new LinkedHashSet<>();
        int ready = 0;
        for (ExamCatalogueItem item : items) {
            courseIds.add(item.getCourseId());
            if (item.getQuestionCount() > 0) {
                ready++;
            }
        }
        totalExamsLabel.setText(String.valueOf(items.size()));
        totalCoursesLabel.setText(String.valueOf(courseIds.size()));
        readyQuizzesLabel.setText(String.valueOf(ready));
    }

    private void applySort() {
        String sort = examSortCombo == null ? "Titre A-Z" : String.valueOf(examSortCombo.getValue());
        if ("Titre Z-A".equalsIgnoreCase(sort)) {
            exams.sort(Comparator.comparing((ExamCatalogueItem item) -> safe(item.getExamTitle()), String.CASE_INSENSITIVE_ORDER).reversed());
        } else if ("Cours A-Z".equalsIgnoreCase(sort)) {
            exams.sort(Comparator.comparing((ExamCatalogueItem item) -> safe(item.getCourseTitle()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(item -> safe(item.getExamTitle()), String.CASE_INSENSITIVE_ORDER));
        } else if ("Questions desc".equalsIgnoreCase(sort)) {
            exams.sort(Comparator.comparingInt(ExamCatalogueItem::getQuestionCount).reversed()
                    .thenComparing(item -> safe(item.getExamTitle()), String.CASE_INSENSITIVE_ORDER));
        } else {
            exams.sort(Comparator.comparing((ExamCatalogueItem item) -> safe(item.getExamTitle()), String.CASE_INSENSITIVE_ORDER));
        }
    }

    @FXML
    private void newExam(ActionEvent event) {
        editingExam = null;
        if (examsTable != null) {
            examsTable.getSelectionModel().clearSelection();
        }
        fillExamForm(null);
    }

    @FXML
    private void editExam(ActionEvent event) {
        ExamCatalogueItem selected = currentExam();
        if (selected == null) {
            info("Examen", "Selectionnez un examen a modifier.");
            return;
        }
        fillExamForm(selected);
        examTitleField.requestFocus();
        examTitleField.selectAll();
    }

    @FXML
    private void saveExam(ActionEvent event) {
        ExamCatalogueItem item = editingExam == null ? new ExamCatalogueItem() : editingExam;
        item.setExamTitle(text(examTitleField));
        item.setExamDescription(text(examDescriptionArea));
        item.setLevelLabel(text(levelField));
        item.setDomainLabel(text(domainField));
        if (editingExam == null) {
            item.setPublished(false);
        }

        // Validation
        if (item.getExamTitle().isBlank()) {
            info("Examen", "Le titre de l'examen est obligatoire.");
            return;
        }
        if (item.getExamTitle().length() > 200) {
            info("Examen", "Le titre ne doit pas depasser 200 caracteres.");
            return;
        }
        if (item.getExamDescription().length() > 2000) {
            info("Examen", "La description est trop longue (max 2000 caracteres).");
            return;
        }
        if (item.getLevelLabel().length() > 100 || item.getDomainLabel().length() > 100) {
            info("Examen", "Niveau ou domaine trop long (max 100 caracteres).");
            return;
        }

        try {
            int cid = Integer.parseInt(text(courseIdField));
            if (cid <= 0) {
                info("Examen", "Le champ Cours ID doit etre un entier positif.");
                return;
            }
            item.setCourseId(cid);
        } catch (Exception e) {
            info("Examen", "Le champ Cours ID doit etre un entier valide.");
            return;
        }

        try {
            if (item.getExamId() <= 0) {
                repository.addExam(item);
                info("Examen", "Examen ajoute.");
            } else {
                repository.updateExam(item);
                info("Examen", "Examen mis a jour.");
            }
            reload();
            selectExamById(item.getExamId());
        } catch (Exception e) {
            error("Erreur examen", e);
        }
    }

    @FXML
    private void togglePublishExam(ActionEvent event) {
        if (!com.educompus.app.AppState.isAdmin()) {
            info("Permission", "Seul l'administrateur peut publier/depublier cet examen.");
            return;
        }
        ExamCatalogueItem selected = currentExam();
        if (selected == null) {
            info("Publication", "Selectionnez un examen.");
            return;
        }

        String action = selected.isPublished() ? "depublier" : "publier";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText((selected.isPublished() ? "Depublier" : "Publier") + " l'examen ?");
        confirm.setContentText("Examen: " + safe(selected.getExamTitle()) + "\nVoulez-vous vraiment " + action + " cet examen ?");
        styleDialog(confirm);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            repository.setPublished(selected.getExamId(), !selected.isPublished());
            selected.setPublished(!selected.isPublished());
            info("Publication", selected.isPublished() ? "Examen publie." : "Examen depublie.");
            if (examsTable != null) {
                examsTable.refresh();
            }
            reload();
        } catch (Exception e) {
            error("Erreur publication examen", e);
        }
    }

    @FXML
    private void deleteExam(ActionEvent event) {
        ExamCatalogueItem selected = currentExam();
        if (selected == null) {
            info("Examen", "Selectionnez un examen a supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer examen");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Examen: " + selected.getExamTitle());
        styleDialog(confirm);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            repository.deleteExam(selected.getExamId());
            info("Examen", "Examen supprime.");
            reload();
        } catch (Exception e) {
            error("Erreur suppression examen", e);
        }
    }

    @FXML
    private void openQuestionsPage(ActionEvent event) {
        ExamCatalogueItem selected = currentExam();
        if (selected == null) {
            info("Questions", "Selectionnez d'abord un examen.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/back/BackExamQuestions.fxml"));
            Parent root = loader.load();
            BackExamQuestionsController controller = loader.getController();
            if (controller != null) {
                controller.setExam(selected);
            }

            Window owner = examsTable == null || examsTable.getScene() == null ? null : examsTable.getScene().getWindow();
            Stage stage = new Stage();
            stage.setTitle("Questions & reponses - " + selected.getExamTitle());
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) {
                stage.initOwner(owner);
            }

            Scene scene = new Scene(root, 1180, 760);
            if (examsTable != null && examsTable.getScene() != null) {
                scene.getStylesheets().setAll(examsTable.getScene().getStylesheets());
            }
            Theme.apply(root);
            stage.setScene(scene);
            stage.showAndWait();
            reload();
        } catch (Exception e) {
            error("Erreur questions", e);
        }
    }

    private ExamCatalogueItem currentExam() {
        return examsTable == null ? editingExam : examsTable.getSelectionModel().getSelectedItem();
    }

    private void fillExamForm(ExamCatalogueItem item) {
        examTitleField.setText(item == null ? "" : safe(item.getExamTitle()));
        examDescriptionArea.setText(item == null ? "" : safe(item.getExamDescription()));
        courseIdField.setText(item == null ? "" : String.valueOf(item.getCourseId()));
        levelField.setText(item == null ? "" : safe(item.getLevelLabel()));
        domainField.setText(item == null ? "" : safe(item.getDomainLabel()));
        selectedExamLabel.setText(item == null
                ? "Nouvel examen"
                : "Examen #" + item.getExamId() + " - " + summarize(item.getExamTitle(), 38)
                + (item.isPublished() ? " [Publie]" : " [Brouillon]"));
    }

    private void selectExamById(int examId) {
        for (ExamCatalogueItem item : exams) {
            if (item.getExamId() == examId) {
                examsTable.getSelectionModel().select(item);
                examsTable.scrollTo(item);
                return;
            }
        }
    }

    private static String text(TextField field) {
        return field == null ? "" : safe(field.getText());
    }

    private static String text(TextArea area) {
        return area == null ? "" : safe(area.getText());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String summarize(String value, int max) {
        String clean = safe(value).replace('\n', ' ').replace('\r', ' ');
        if (clean.length() <= max) {
            return clean;
        }
        return clean.substring(0, Math.max(0, max - 1)).trim() + "...";
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
        if (e != null) {
            e.printStackTrace();
        }
    }

    private static void styleDialog(javafx.scene.control.Dialog<?> dialog) {
        if (dialog == null || dialog.getDialogPane() == null) {
            return;
        }
        String css = cssUri();
        if (!css.isBlank() && !dialog.getDialogPane().getStylesheets().contains(css)) {
            dialog.getDialogPane().getStylesheets().add(css);
        }
        if (!dialog.getDialogPane().getStyleClass().contains("rgb-dialog")) {
            dialog.getDialogPane().getStyleClass().add("rgb-dialog");
        }
        for (ButtonType buttonType : dialog.getDialogPane().getButtonTypes()) {
            Node button = dialog.getDialogPane().lookupButton(buttonType);
            if (button == null) {
                continue;
            }
            if (buttonType == ButtonType.OK) {
                button.getStyleClass().add("btn-rgb");
            } else if (buttonType == ButtonType.CANCEL) {
                button.getStyleClass().add("btn-rgb-outline");
            }
        }
    }

    private static String cssUri() {
        File file = new File("styles/educompus.css");
        if (!file.exists()) {
            file = new File("eduCompus-javafx/styles/educompus.css");
        }
        if (!file.exists()) {
            file = new File(new File("..", "eduCompus-javafx"), "styles/educompus.css");
        }
        return file.exists() ? file.toURI().toString() : "";
    }
}

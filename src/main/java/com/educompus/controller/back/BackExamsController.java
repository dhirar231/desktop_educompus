package com.educompus.controller.back;

import com.educompus.model.ExamCatalogueItem;
import com.educompus.model.Cours;
import com.educompus.repository.ExamRepository;
import com.educompus.repository.CourseManagementRepository;
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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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
    private ListView<ExamCatalogueItem> examsList;
    @FXML
    private Button examsPrevBtn;
    @FXML
    private Button examsNextBtn;
    @FXML
    private Label examsPageLabel;
    @FXML
    private TextField examTitleField;
    @FXML
    private javafx.scene.control.ComboBox<Cours> courseCombo;
    @FXML
    private TextField levelField;
    @FXML
    private TextField domainField;
    @FXML
    private TextArea examDescriptionArea;
    
    @FXML
    private Label examTitleError;
    @FXML
    private Label courseError;
    @FXML
    private Label levelError;
    @FXML
    private Label domainError;
    @FXML
    private Label examDescriptionError;
    @FXML
    private Label selectedExamLabel;
    @FXML
    private javafx.scene.control.Button saveExamBtn;

    private final ExamRepository repository = new ExamRepository();
    private final ObservableList<ExamCatalogueItem> exams = FXCollections.observableArrayList();
    private final List<ExamCatalogueItem> allExams = new java.util.ArrayList<>();
    private int examsPageSize = 10;
    private int examsCurrentPage = 1;
    private ExamCatalogueItem editingExam;
    private final CourseManagementRepository courseRepo = new CourseManagementRepository();
    private final javafx.collections.ObservableList<Cours> courses = FXCollections.observableArrayList();


    @FXML
    private void initialize() {
        setupList();
        setupSort();
        loadCourses();
        setupValidation();
        reload();
    }

    private void setupValidation() {
        // respond to field changes to enable/disable save button
        Runnable updater = this::updateSaveButtonState;
        if (examTitleField != null) examTitleField.textProperty().addListener((obs, o, n) -> updater.run());
        if (examDescriptionArea != null) examDescriptionArea.textProperty().addListener((obs, o, n) -> updater.run());
        if (levelField != null) levelField.textProperty().addListener((obs, o, n) -> updater.run());
        if (domainField != null) domainField.textProperty().addListener((obs, o, n) -> updater.run());
        if (courseCombo != null) courseCombo.valueProperty().addListener((obs, o, n) -> updater.run());
        // initial state
        updateSaveButtonState();
    }

    private void updateSaveButtonState() {
        boolean titleError = examTitleField == null || safe(examTitleField.getText()).isBlank() || safe(examTitleField.getText()).length() > 200;
        boolean descError = examDescriptionArea == null || safe(examDescriptionArea.getText()).isBlank() || safe(examDescriptionArea.getText()).length() > 2000;
        boolean lvlError = levelField == null || safe(levelField.getText()).isBlank() || safe(levelField.getText()).length() > 100;
        boolean domError = domainField == null || safe(domainField.getText()).isBlank() || safe(domainField.getText()).length() > 100;
        boolean courseSelError = courseCombo != null && courseCombo.getValue() == null;

        boolean valid = !titleError && !descError && !lvlError && !domError && !courseSelError;
        if (saveExamBtn != null) saveExamBtn.setDisable(!valid);

        setFieldError(examTitleField, titleError);
        setFieldError(examDescriptionArea, descError);
        setFieldError(levelField, lvlError);
        setFieldError(domainField, domError);
        setFieldError(courseCombo, courseSelError);

        if (examTitleError != null) {
            examTitleError.setVisible(titleError);
            examTitleError.setManaged(titleError);
            examTitleError.setText(titleError ? (safe(examTitleField.getText()).isBlank() ? "Le titre est obligatoire." : "Max 200 caracteres.") : "");
        }
        if (examDescriptionError != null) {
            examDescriptionError.setVisible(descError);
            examDescriptionError.setManaged(descError);
            if (safe(examDescriptionArea.getText()).isBlank()) {
                examDescriptionError.setText("La description est obligatoire.");
            } else if (safe(examDescriptionArea.getText()).length() > 2000) {
                examDescriptionError.setText("Description trop longue (max 2000).");
            } else {
                examDescriptionError.setText("");
            }
        }
        if (levelError != null) {
            levelError.setVisible(lvlError);
            levelError.setManaged(lvlError);
            if (safe(levelField.getText()).isBlank()) {
                levelError.setText("Le niveau est obligatoire.");
            } else if (safe(levelField.getText()).length() > 100) {
                levelError.setText("Niveau trop long (max 100).");
            } else {
                levelError.setText("");
            }
        }
        if (domainError != null) {
            domainError.setVisible(domError);
            domainError.setManaged(domError);
            if (safe(domainField.getText()).isBlank()) {
                domainError.setText("Le domaine est obligatoire.");
            } else if (safe(domainField.getText()).length() > 100) {
                domainError.setText("Domaine trop long (max 100)." );
            } else {
                domainError.setText("");
            }
        }
        if (courseError != null) {
            courseError.setVisible(courseSelError);
            courseError.setManaged(courseSelError);
            courseError.setText(courseSelError ? "Selectionnez un cours." : "");
        }
    }

    private boolean isExamFormValid(boolean showAlerts) {
        String title = text(examTitleField);
        if (title.isBlank()) {
            if (showAlerts) info("Examen", "Le titre de l'examen est obligatoire.");
            return false;
        }
        if (title.length() > 200) {
            if (showAlerts) info("Examen", "Le titre ne doit pas depasser 200 caracteres.");
            return false;
        }
        String desc = text(examDescriptionArea);
        if (desc.isBlank()) {
            if (showAlerts) info("Examen", "La description est obligatoire.");
            return false;
        }
        if (desc.length() > 2000) {
            if (showAlerts) info("Examen", "La description est trop longue (max 2000 caracteres).");
            return false;
        }
        String lvl = text(levelField);
        if (lvl.isBlank()) {
            if (showAlerts) info("Examen", "Le niveau est obligatoire.");
            return false;
        }
        if (lvl.length() > 100) {
            if (showAlerts) info("Examen", "Niveau trop long (max 100 caracteres).");
            return false;
        }
        String dom = text(domainField);
        if (dom.isBlank()) {
            if (showAlerts) info("Examen", "Le domaine est obligatoire.");
            return false;
        }
        if (dom.length() > 100) {
            if (showAlerts) info("Examen", "Domaine trop long (max 100 caracteres).");
            return false;
        }
        if (courseCombo == null || courseCombo.getValue() == null) {
            if (showAlerts) info("Examen", "Veuillez selectionner un cours valide.");
            return false;
        }
        return true;
    }

    private void setFieldError(javafx.scene.Node node, boolean error) {
        if (node == null) return;
        var cls = node.getStyleClass();
        if (error) {
            if (!cls.contains("field-error")) cls.add("field-error");
        } else {
            cls.removeAll("field-error");
        }
    }
    private void setupList() {
        examsList.setItems(exams);
        examsList.setCellFactory(lv -> new ListCell<>() {
            private final Label title = new Label();
            private final Label course = new Label();
            private final HBox box = new HBox(8);
            {
                VBox v = new VBox(4, title, course);
                Region spacer = new Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                box.getChildren().addAll(v, spacer);
            }

            @Override
            protected void updateItem(ExamCatalogueItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                title.setText(safe(item.getExamTitle()));
                course.setText(safe(item.getCourseTitle()));
                setGraphic(box);
                setText(null);
            }
        });

        examsList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            editingExam = newValue;
            fillExamForm(newValue);
        });
    }

    private void loadCourses() {
        try {
            java.util.List<Cours> list = courseRepo.listCours("");
            courses.setAll(list);
            if (courseCombo != null) {
                courseCombo.getItems().setAll(courses);
            }
        } catch (Exception e) {
            error("Cours", e instanceof Exception ? (Exception) e : null);
        }
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
        allExams.clear();
        allExams.addAll(items);
        applySort();
        updateStats(items);
        // refresh course list for combobox
        loadCourses();

        int selectedId = editingExam == null ? 0 : editingExam.getExamId();
        if (selectedId > 0) {
            selectExamById(selectedId);
        }
        // show first page
        examsCurrentPage = 1;
        showExamsPage(examsCurrentPage);
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
            allExams.sort(Comparator.comparing((ExamCatalogueItem item) -> safe(item.getExamTitle()), String.CASE_INSENSITIVE_ORDER).reversed());
        } else if ("Cours A-Z".equalsIgnoreCase(sort)) {
            allExams.sort(Comparator.comparing((ExamCatalogueItem item) -> safe(item.getCourseTitle()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(item -> safe(item.getExamTitle()), String.CASE_INSENSITIVE_ORDER));
        } else if ("Questions desc".equalsIgnoreCase(sort)) {
            allExams.sort(Comparator.comparingInt(ExamCatalogueItem::getQuestionCount).reversed()
                    .thenComparing(item -> safe(item.getExamTitle()), String.CASE_INSENSITIVE_ORDER));
        } else {
            allExams.sort(Comparator.comparing((ExamCatalogueItem item) -> safe(item.getExamTitle()), String.CASE_INSENSITIVE_ORDER));
        }
        // refresh page after sorting
        showExamsPage(examsCurrentPage <= 0 ? 1 : examsCurrentPage);
    }

    @FXML
    private void onExamsPrev(ActionEvent ev) {
        if (examsCurrentPage > 1) showExamsPage(examsCurrentPage - 1);
    }

    @FXML
    private void onExamsNext(ActionEvent ev) {
        int total = (int) Math.max(1, Math.ceil((double) allExams.size() / examsPageSize));
        if (examsCurrentPage < total) showExamsPage(examsCurrentPage + 1);
    }

    private void showExamsPage(int page) {
        examsCurrentPage = Math.max(1, page);
        int total = (int) Math.max(1, Math.ceil((double) allExams.size() / examsPageSize));
        int from = (examsCurrentPage - 1) * examsPageSize;
        int to = Math.min(allExams.size(), from + examsPageSize);
        List<ExamCatalogueItem> sub = new java.util.ArrayList<>();
        if (from < to) sub.addAll(allExams.subList(from, to));
        exams.setAll(sub);
        if (examsList != null && !exams.isEmpty() && examsList.getSelectionModel().getSelectedItem() == null) examsList.getSelectionModel().selectFirst();
        if (examsPageLabel != null) examsPageLabel.setText("Page " + examsCurrentPage + " / " + total);
        if (examsPrevBtn != null) examsPrevBtn.setDisable(examsCurrentPage <= 1);
        if (examsNextBtn != null) examsNextBtn.setDisable(examsCurrentPage >= total);
    }

    @FXML
    private void newExam(ActionEvent event) {
        editingExam = null;
        if (examsList != null) {
            examsList.getSelectionModel().clearSelection();
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
        // New exams are created as draft (Brouillon) by default — admin will publish when ready
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

        if (courseCombo == null || courseCombo.getValue() == null) {
            info("Examen", "Veuillez selectionner un cours valide.");
            return;
        }
        item.setCourseId(courseCombo.getValue().getId());

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
            if (examsList != null) {
                examsList.refresh();
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

            Window owner = examsList == null || examsList.getScene() == null ? null : examsList.getScene().getWindow();
            Stage stage = new Stage();
            stage.setTitle("Questions & reponses - " + selected.getExamTitle());
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) {
                stage.initOwner(owner);
            }

            Scene scene = new Scene(root, 1180, 760);
            if (examsList != null && examsList.getScene() != null) {
                scene.getStylesheets().setAll(examsList.getScene().getStylesheets());
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
        return examsList == null ? editingExam : examsList.getSelectionModel().getSelectedItem();
    }

    private void fillExamForm(ExamCatalogueItem item) {
        examTitleField.setText(item == null ? "" : safe(item.getExamTitle()));
        examDescriptionArea.setText(item == null ? "" : safe(item.getExamDescription()));
        if (courseCombo != null) {
            if (item == null) {
                courseCombo.setValue(null);
            } else {
                Cours found = null;
                for (Cours c : courses) {
                    if (c.getId() == item.getCourseId()) {
                        found = c;
                        break;
                    }
                }
                courseCombo.setValue(found);
            }
        }
        levelField.setText(item == null ? "" : safe(item.getLevelLabel()));
        domainField.setText(item == null ? "" : safe(item.getDomainLabel()));
        selectedExamLabel.setText(item == null
                ? "Nouvel examen"
                : "Examen #" + item.getExamId() + " - " + summarize(item.getExamTitle(), 38)
                + (item.isPublished() ? " [Publie]" : " [Brouillon]"));
        // publishedToggle removed: published state shown via selectedExamLabel
    }

    private void selectExamById(int examId) {
        for (ExamCatalogueItem item : exams) {
            if (item.getExamId() == examId) {
                examsList.getSelectionModel().select(item);
                examsList.scrollTo(item);
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

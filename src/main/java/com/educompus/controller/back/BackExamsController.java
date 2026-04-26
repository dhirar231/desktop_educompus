package com.educompus.controller.back;

import com.educompus.model.Cours;
import com.educompus.model.ExamCatalogueItem;
import com.educompus.repository.CourseManagementRepository;
import com.educompus.repository.ExamRepository;
import com.educompus.service.ExamValidationService;
import com.educompus.service.FormValidator;
import com.educompus.service.ValidationResult;
import com.educompus.util.Dialogs;
import com.educompus.util.Theme;
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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.FileChooser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class BackExamsController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> examSortCombo;
    @FXML private Label totalExamsLabel;
    @FXML private Label totalCoursesLabel;
    @FXML private Label readyQuizzesLabel;
    @FXML private ListView<ExamCatalogueItem> examsList;
    @FXML private Button examsPrevBtn;
    @FXML private Button examsNextBtn;
    @FXML private Label examsPageLabel;

    private final ExamRepository repository = new ExamRepository();
    private final CourseManagementRepository courseRepo = new CourseManagementRepository();
    private final ObservableList<ExamCatalogueItem> exams = FXCollections.observableArrayList();
    private final ObservableList<Cours> courses = FXCollections.observableArrayList();
    private final List<ExamCatalogueItem> allExams = new ArrayList<>();
    private int examsPageSize = 10;
    private int examsCurrentPage = 1;
    private ExamCatalogueItem editingExam;

    @FXML
    private void initialize() {
        setupList();
        setupSort();
        loadCourses();
        reload();
    }

    private void setupList() {
        if (examsList == null) return;
        examsList.setItems(exams);
        examsList.setCellFactory(lv -> new ListCell<>() {
            private final Label titleLabel = new Label();
            private final Label metaLabel = new Label();
            private final HBox row;
            {
                titleLabel.getStyleClass().add("project-card-title");
                metaLabel.getStyleClass().add("page-subtitle");
                metaLabel.setStyle("-fx-font-size: 11px;");
                VBox info = new VBox(4, titleLabel, metaLabel);
                HBox.setHgrow(info, Priority.ALWAYS);
                row = new HBox(10, info);
                row.setPadding(new javafx.geometry.Insets(10, 12, 10, 12));
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(ExamCatalogueItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                titleLabel.setText(safe(item.getExamTitle()));
                metaLabel.setText(
                        safe(item.getCourseTitle()) + "  •  " +
                        safe(item.getLevelLabel()) + "  •  " +
                        safe(item.getDomainLabel()) + "  •  " +
                        item.getQuestionCount() + " questions  •  " +
                        (item.isPublished() ? "Publié" : "Brouillon")
                );
                setText(null);
                setGraphic(row);
            }
        });
        examsList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> editingExam = newValue);
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

    private void loadCourses() {
        try {
            courses.setAll(courseRepo.listCours(""));
        } catch (Exception e) {
            error("Cours", e);
        }
    }

    @FXML
    private void reload() {
        loadCourses();
        allExams.clear();
        allExams.addAll(repository.listAdminRows(text(searchField)));
        updateStats(allExams);
        applySort();
    }

    private void updateStats(List<ExamCatalogueItem> items) {
        Set<Integer> courseIds = new LinkedHashSet<>();
        int ready = 0;
        for (ExamCatalogueItem item : items) {
            courseIds.add(item.getCourseId());
            if (item.getQuestionCount() > 0) ready++;
        }
        if (totalExamsLabel != null) totalExamsLabel.setText(String.valueOf(items.size()));
        if (totalCoursesLabel != null) totalCoursesLabel.setText(String.valueOf(courseIds.size()));
        if (readyQuizzesLabel != null) readyQuizzesLabel.setText(String.valueOf(ready));
    }

    private void applySort() {
        String sort = examSortCombo == null ? "Titre A-Z" : safe(examSortCombo.getValue());
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
        showExamsPage(examsCurrentPage <= 0 ? 1 : examsCurrentPage);
    }

    @FXML
    private void onExamsPrev(ActionEvent event) {
        if (examsCurrentPage > 1) showExamsPage(examsCurrentPage - 1);
    }

    @FXML
    private void onExamsNext(ActionEvent event) {
        int total = totalExamPages();
        if (examsCurrentPage < total) showExamsPage(examsCurrentPage + 1);
    }

    private void showExamsPage(int page) {
        examsCurrentPage = Math.max(1, page);
        int total = totalExamPages();
        int from = (examsCurrentPage - 1) * examsPageSize;
        int to = Math.min(allExams.size(), from + examsPageSize);
        List<ExamCatalogueItem> sub = new ArrayList<>();
        if (from < to) sub.addAll(allExams.subList(from, to));
        exams.setAll(sub);
        if (examsList != null && examsList.getSelectionModel().getSelectedItem() == null && !exams.isEmpty()) {
            examsList.getSelectionModel().selectFirst();
        }
        if (examsPageLabel != null) examsPageLabel.setText("Page " + examsCurrentPage + " / " + total);
        if (examsPrevBtn != null) examsPrevBtn.setDisable(examsCurrentPage <= 1);
        if (examsNextBtn != null) examsNextBtn.setDisable(examsCurrentPage >= total);
    }

    private int totalExamPages() {
        return (int) Math.max(1, Math.ceil((double) allExams.size() / examsPageSize));
    }

    @FXML
    private void newExam(ActionEvent event) {
        openExamDialog(null);
    }

    public void triggerNewExam() {
        openExamDialog(null);
    }

    @FXML
    private void editExam(ActionEvent event) {
        ExamCatalogueItem selected = currentExam();
        if (selected == null) {
            info("Examen", "Sélectionnez un examen à modifier.");
            return;
        }
        openExamDialog(selected);
    }

    public void triggerEditExam() {
        editExam(null);
    }

    @FXML
    private void deleteExam(ActionEvent event) {
        ExamCatalogueItem selected = currentExam();
        if (selected == null) {
            info("Examen", "Sélectionnez un examen à supprimer.");
            return;
        }
        if (!confirm("Supprimer examen", "Examen : " + safe(selected.getExamTitle()))) {
            return;
        }
        try {
            repository.deleteExam(selected.getExamId());
            info("Examen", "Examen supprimé.");
            reload();
        } catch (Exception e) {
            error("Erreur suppression examen", e);
        }
    }

    public void triggerDeleteExam() {
        deleteExam(null);
    }

    @FXML
    private void openQuestionsPage(ActionEvent event) {
        ExamCatalogueItem selected = currentExam();
        if (selected == null) {
            info("Questions", "Sélectionnez d'abord un examen.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/back/BackExamQuestions.fxml"));
            Parent root = loader.load();
            BackExamQuestionsController controller = loader.getController();
            if (controller != null) controller.setExam(selected);

            Window owner = examsList == null || examsList.getScene() == null ? null : examsList.getScene().getWindow();
            Stage stage = new Stage();
            stage.setTitle("Questions & réponses - " + selected.getExamTitle());
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) stage.initOwner(owner);

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

    @FXML
    private void importFromExcel(ActionEvent event) {
        Window owner = examsList == null || examsList.getScene() == null ? null : examsList.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importer examens depuis Excel");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"));
        java.io.File file = chooser.showOpenDialog(owner);
        if (file == null) return;

        int courseId = 0;
        ExamCatalogueItem selected = currentExam();
        if (selected != null) {
            courseId = selected.getCourseId();
        } else {
            ComboBox<Cours> combo = comboCours();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Choisir le cours cible");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.getDialogPane().setContent(combo);
            Dialogs.style(dialog);
            java.util.Optional<ButtonType> res = dialog.showAndWait();
            if (res.isEmpty() || res.get().getButtonData() != javafx.scene.control.ButtonBar.ButtonData.OK_DONE) return;
            Cours c = combo.getValue();
            if (c == null) { info("Import", "Aucun cours sélectionné."); return; }
            courseId = c.getId();
        }

        try {
            String summary = com.educompus.util.ExcelExamImporter.importFromExcel(file, courseId);
            String cleaned = summary == null ? "" : summary;
            // remove any explicit debug-log reference
            cleaned = cleaned.replaceAll("\\(.*excel_import_debug\\.log.*\\)", "").trim();

            // try to extract counts and show a concise message
            Pattern p = Pattern.compile("examens?\\s*[:\\-]?\\s*(\\d+).*questions?\\s*[:\\-]?\\s*(\\d+).*r[eé]ponses?\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher m = p.matcher(cleaned);
            if (m.find()) {
                String msg = String.format("Import terminé — examens: %s, questions: %s, réponses: %s", m.group(1), m.group(2), m.group(3));
                info("Importation", msg);
            } else {
                if (cleaned.isBlank()) cleaned = "Import terminé.";
                info("Importation", cleaned);
            }
            reload();
        } catch (Exception e) {
            error("Importation échouée", e);
        }
    }

    public void triggerOpenQuestionsPage() {
        openQuestionsPage(null);
    }

    private void openExamDialog(ExamCatalogueItem source) {
        FormResult<ExamCatalogueItem> result = showExamForm(source);
        if (!result.saved()) return;

        try {
            if (result.value().getExamId() <= 0) {
                repository.addExam(result.value());
                info("Examen", "Examen ajouté.");
            } else {
                repository.updateExam(result.value());
                info("Examen", "Examen mis à jour.");
            }
            reload();
            selectExamById(result.value().getExamId());
        } catch (Exception e) {
            error("Erreur examen", e);
        }
    }

    private FormResult<ExamCatalogueItem> showExamForm(ExamCatalogueItem source) {
        TextField titleField = field();
        ComboBox<Cours> courseCombo = comboCours();
        TextField levelField = field();
        TextField domainField = field();
        TextArea descriptionArea = area();

        if (source != null) {
            titleField.setText(safe(source.getExamTitle()));
            levelField.setText(safe(source.getLevelLabel()));
            domainField.setText(safe(source.getDomainLabel()));
            descriptionArea.setText(safe(source.getExamDescription()));
            selectCourse(courseCombo, source.getCourseId());
        }

        GridPane grid = formGrid();
        Label titleError = addRow(grid, 0, "Titre *", titleField);
        Label courseError = addRow(grid, 1, "Cours *", courseCombo);
        Label levelError = addRow(grid, 2, "Niveau *", levelField);
        Label domainError = addRow(grid, 3, "Domaine *", domainField);
        Label descriptionError = addRow(grid, 4, "Description *", descriptionArea);

        liveValidate(titleField, titleError, () -> ExamValidationService.validateTitle(titleField.getText()));
        liveValidate(levelField, levelError, () -> ExamValidationService.validateLevel(levelField.getText()));
        liveValidate(domainField, domainError, () -> ExamValidationService.validateDomain(domainField.getText()));
        liveValidate(descriptionArea, descriptionError, () -> ExamValidationService.validateDescription(descriptionArea.getText()));
        courseCombo.valueProperty().addListener((obs, oldValue, newValue) ->
                applyValidation(courseCombo, courseError, ExamValidationService.validateCourse(newValue)));

        Dialog<ButtonType> dialog = buildFormDialog(source == null ? "Créer un examen" : "Modifier un examen", grid);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            ValidationResult titleResult = ExamValidationService.validateTitle(titleField.getText());
            ValidationResult courseResult = ExamValidationService.validateCourse(courseCombo.getValue());
            ValidationResult levelResult = ExamValidationService.validateLevel(levelField.getText());
            ValidationResult domainResult = ExamValidationService.validateDomain(domainField.getText());
            ValidationResult descriptionResult = ExamValidationService.validateDescription(descriptionArea.getText());

            applyValidation(titleField, titleError, titleResult);
            applyValidation(courseCombo, courseError, courseResult);
            applyValidation(levelField, levelError, levelResult);
            applyValidation(domainField, domainError, domainResult);
            applyValidation(descriptionArea, descriptionError, descriptionResult);

            if (!(titleResult.isValid() && courseResult.isValid() && levelResult.isValid()
                    && domainResult.isValid() && descriptionResult.isValid())) {
                event.consume();
            }
        });

        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return FormResult.cancelled();
        }

        ExamCatalogueItem item = source == null ? new ExamCatalogueItem() : source;
        item.setExamTitle(text(titleField));
        item.setExamDescription(text(descriptionArea));
        item.setLevelLabel(text(levelField));
        item.setDomainLabel(text(domainField));
        item.setCourseId(courseCombo.getValue().getId());
        if (source == null) item.setPublished(false);
        return FormResult.saved(item);
    }

    private Dialog<ButtonType> buildFormDialog(String title, Node content) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(760);
        Dialogs.style(dialog);
        return dialog;
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(4);
        return grid;
    }

    private Label addRow(GridPane grid, int row, String label, Node node) {
        Label formLabel = new Label(label);
        formLabel.getStyleClass().add("form-label");
        grid.add(formLabel, 0, row * 2);
        grid.add(node, 1, row * 2);
        if (node instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(region, Priority.ALWAYS);
        }
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #d6293e; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 0 0 4 2;");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(420);
        grid.add(errorLabel, 1, row * 2 + 1);
        return errorLabel;
    }

    private void liveValidate(TextInputControl field, Label errorLabel, java.util.function.Supplier<ValidationResult> validator) {
        field.textProperty().addListener((obs, oldValue, newValue) -> applyValidation(field, errorLabel, validator.get()));
    }

    private void applyValidation(javafx.scene.control.Control field, Label errorLabel, ValidationResult result) {
        if (result == null || result.isValid()) {
            errorLabel.setText("");
            FormValidator.clearError(field);
        } else {
            errorLabel.setText("⚠ " + result.firstError());
            FormValidator.markError(field, result.firstError());
        }
    }

    private TextField field() {
        TextField field = new TextField();
        field.getStyleClass().add("field");
        return field;
    }

    private TextArea area() {
        TextArea area = new TextArea();
        area.getStyleClass().addAll("field", "area");
        area.setPrefRowCount(6);
        area.setWrapText(true);
        return area;
    }

    private ComboBox<Cours> comboCours() {
        ComboBox<Cours> combo = new ComboBox<>();
        combo.getStyleClass().addAll("field", "combo-box");
        combo.setItems(FXCollections.observableArrayList(courses));
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Cours item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : safe(item.getTitre()));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Cours item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : safe(item.getTitre()));
            }
        });
        return combo;
    }

    private void selectCourse(ComboBox<Cours> combo, int courseId) {
        for (Cours cours : combo.getItems()) {
            if (cours.getId() == courseId) {
                combo.setValue(cours);
                return;
            }
        }
    }

    private ExamCatalogueItem currentExam() {
        return examsList == null ? editingExam : examsList.getSelectionModel().getSelectedItem();
    }

    private void selectExamById(int examId) {
        if (examId <= 0) return;
        int index = -1;
        for (int i = 0; i < allExams.size(); i++) {
            if (allExams.get(i).getExamId() == examId) {
                index = i;
                break;
            }
        }
        if (index < 0) return;
        int targetPage = (index / examsPageSize) + 1;
        showExamsPage(targetPage);
        for (ExamCatalogueItem item : exams) {
            if (item.getExamId() == examId) {
                examsList.getSelectionModel().select(item);
                examsList.scrollTo(item);
                editingExam = item;
                return;
            }
        }
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        Dialogs.style(alert);
        return alert.showAndWait().map(button -> button == ButtonType.OK).orElse(false);
    }

    private void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Dialogs.style(alert);
        alert.showAndWait();
    }

    private void error(String title, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(e == null ? "Erreur" : safe(e.getMessage()));
        Dialogs.style(alert);
        alert.showAndWait();
        if (e != null) e.printStackTrace();
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

    private record FormResult<T>(T value, boolean saved) {
        static <T> FormResult<T> saved(T value) { return new FormResult<>(value, true); }
        static <T> FormResult<T> cancelled() { return new FormResult<>(null, false); }
    }
}

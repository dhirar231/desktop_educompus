package com.educompus.controller.back;

import com.educompus.model.ExamAnswer;
import com.educompus.model.ExamCatalogueItem;
import com.educompus.model.ExamQuestion;
import com.educompus.repository.ExamRepository;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.File;
import java.util.List;

public final class BackExamQuestionsController {
    @FXML
    private Label examContextLabel;
    @FXML
    private TableView<ExamQuestion> questionsTable;
    @FXML
    private TableColumn<ExamQuestion, Number> colQuestionId;
    @FXML
    private TableColumn<ExamQuestion, String> colQuestionText;
    @FXML
    private TableColumn<ExamQuestion, Number> colQuestionDuration;
    @FXML
    private TableColumn<ExamQuestion, Number> colQuestionAnswers;
    @FXML
    private TextArea questionTextArea;
    @FXML
    private TextField durationField;
    @FXML
    private Label selectedQuestionLabel;
    @FXML
    private TableView<ExamAnswer> answersTable;
    @FXML
    private TableColumn<ExamAnswer, String> colAnswerText;
    @FXML
    private TableColumn<ExamAnswer, String> colAnswerCorrect;
    @FXML
    private TextField answerTextField;
    @FXML
    private Label answerHintLabel;
    @FXML
    private Button answerCorrectBtn;
    @FXML
    private Button answerWrongBtn;

    private final ExamRepository repository = new ExamRepository();
    private final ObservableList<ExamQuestion> questions = FXCollections.observableArrayList();
    private final ObservableList<ExamAnswer> answers = FXCollections.observableArrayList();

    private ExamCatalogueItem exam;
    private ExamQuestion editingQuestion;
    private ExamAnswer editingAnswer;
    private boolean answerCorrect;

    @FXML
    private void initialize() {
        setupQuestionsTable();
        setupAnswersTable();
        fillQuestionForm(null);
    }

    public void setExam(ExamCatalogueItem exam) {
        this.exam = exam;
        if (examContextLabel != null) {
            examContextLabel.setText(exam == null
                    ? "Selectionnez un examen"
                    : "Examen #" + exam.getExamId() + " - " + exam.getExamTitle());
        }
        loadQuestions();
    }

    private void setupQuestionsTable() {
        questionsTable.setItems(questions);
        colQuestionId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()));
        colQuestionText.setCellValueFactory(data -> new SimpleStringProperty(summarize(data.getValue().getText(), 110)));
        colQuestionDuration.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getDurationSeconds()));
        colQuestionAnswers.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getAnswers().size()));
        colQuestionId.setVisible(false);
        colQuestionDuration.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.intValue() + " s");
            }
        });

        questionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            editingQuestion = newValue;
            fillQuestionForm(newValue);
        });
    }

    private void setupAnswersTable() {
        answersTable.setItems(answers);
        colAnswerText.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getText()));
        colAnswerCorrect.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isCorrect() ? "Oui" : "Non"));
        colAnswerCorrect.setCellFactory(col -> new TableCell<>() {
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
                badge.getStyleClass().removeAll("chip-success", "chip-outline");
                badge.getStyleClass().add("Oui".equalsIgnoreCase(item) ? "chip-success" : "chip-outline");
                setGraphic(badge);
                setText(null);
            }
        });

        answersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            editingAnswer = newValue;
            fillAnswerForm(newValue);
        });
    }

    private void loadQuestions() {
        if (exam == null) {
            questions.clear();
            answers.clear();
            fillQuestionForm(null);
            return;
        }
        int selectedQuestionId = editingQuestion == null ? 0 : editingQuestion.getId();
        try {
            questions.setAll(repository.listQuestionsByExamId(exam.getExamId()));
        } catch (Exception e) {
            questions.clear();
            error("Erreur chargement questions", e);
        }
        if (selectedQuestionId > 0) {
            selectQuestionById(selectedQuestionId);
        }
        if (questions.isEmpty()) {
            fillQuestionForm(null);
            answers.clear();
        } else if (questionsTable.getSelectionModel().getSelectedItem() == null) {
            questionsTable.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void newQuestion(ActionEvent event) {
        editingQuestion = null;
        questionsTable.getSelectionModel().clearSelection();
        fillQuestionForm(null);
    }

    @FXML
    private void saveQuestion(ActionEvent event) {
        if (exam == null) {
            info("Question", "Aucun examen selectionne.");
            return;
        }
        String questionText = text(questionTextArea);
        if (questionText.isBlank()) {
            info("Question", "Le texte de la question est obligatoire.");
            return;
        }
        if (questionText.length() > 2000) {
            info("Question", "Le texte de la question est trop long (max 2000 caracteres).");
            return;
        }

        int duration;
        try {
            duration = Integer.parseInt(text(durationField));
            if (duration < 0 || duration > 86400) { // seconds, allow up to 24h
                info("Question", "La duree doit etre entre 0 et 86400 secondes.");
                return;
            }
        } catch (Exception e) {
            info("Question", "La duree doit etre un entier en secondes.");
            return;
        }

        ExamQuestion question = editingQuestion == null ? new ExamQuestion() : editingQuestion;
        question.setText(questionText);
        question.setDurationSeconds(duration);

        try {
            if (question.getId() <= 0) {
                repository.addQuestion(question, exam.getExamId());
                info("Question", "Question ajoutee.");
            } else {
                repository.updateQuestion(question, exam.getExamId());
                info("Question", "Question mise a jour.");
            }
            loadQuestions();
            selectQuestionById(question.getId());
        } catch (Exception e) {
            error("Erreur question", e);
        }
    }

    @FXML
    private void deleteQuestion(ActionEvent event) {
        ExamQuestion question = currentQuestion();
        if (question == null) {
            info("Question", "Selectionnez une question a supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer question");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText(summarize(question.getText(), 120));
        styleDialog(confirm);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            repository.deleteQuestion(question.getId());
            info("Question", "Question supprimee.");
            loadQuestions();
        } catch (Exception e) {
            error("Erreur suppression question", e);
        }
    }

    @FXML
    private void newAnswer(ActionEvent event) {
        if (currentQuestion() == null) {
            info("Reponse", "Selectionnez d'abord une question.");
            return;
        }
        editingAnswer = null;
        answersTable.getSelectionModel().clearSelection();
        fillAnswerForm(null);
    }

    @FXML
    private void saveAnswer(ActionEvent event) {
        ExamQuestion question = currentQuestion();
        if (question == null) {
            info("Reponse", "Selectionnez d'abord une question.");
            return;
        }
        String answerText = text(answerTextField);
        if (answerText.isBlank()) {
            info("Reponse", "Le texte de la reponse est obligatoire.");
            return;
        }

        ExamAnswer answer = editingAnswer == null ? new ExamAnswer() : editingAnswer;
        answer.setText(answerText);
        answer.setCorrect(answerCorrect);

        try {
            if (answer.getId() <= 0) {
                repository.addAnswer(answer, question.getId());
                info("Reponse", "Reponse ajoutee.");
            } else {
                repository.updateAnswer(answer);
                info("Reponse", "Reponse mise a jour.");
            }
            loadQuestions();
            selectQuestionById(question.getId());
            selectAnswerById(answer.getId());
        } catch (Exception e) {
            error("Erreur reponse", e);
        }
    }

    @FXML
    private void deleteAnswer(ActionEvent event) {
        ExamAnswer answer = currentAnswer();
        if (answer == null) {
            info("Reponse", "Selectionnez une reponse a supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer reponse");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText(answer.getText());
        styleDialog(confirm);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        ExamQuestion question = currentQuestion();
        try {
            repository.deleteAnswer(answer.getId());
            info("Reponse", "Reponse supprimee.");
            loadQuestions();
            if (question != null) {
                selectQuestionById(question.getId());
            }
        } catch (Exception e) {
            error("Erreur suppression reponse", e);
        }
    }

    @FXML
    private void markAnswerCorrect(ActionEvent event) {
        setAnswerCorrect(true);
    }

    @FXML
    private void markAnswerWrong(ActionEvent event) {
        setAnswerCorrect(false);
    }

    private void fillQuestionForm(ExamQuestion question) {
        questionTextArea.setText(question == null ? "" : safe(question.getText()));
        durationField.setText(question == null ? "45" : String.valueOf(question.getDurationSeconds()));
        selectedQuestionLabel.setText(question == null
                ? "Nouvelle question"
                : "Question #" + question.getId() + " - " + question.getAnswers().size() + " reponse(s)");
        answers.setAll(question == null ? List.of() : question.getAnswers());
        fillAnswerForm(null);
    }

    private void fillAnswerForm(ExamAnswer answer) {
        answerTextField.setText(answer == null ? "" : safe(answer.getText()));
        setAnswerCorrect(answer != null && answer.isCorrect());
    }

    private void setAnswerCorrect(boolean correct) {
        answerCorrect = correct;
        updateToggleButton(answerCorrectBtn, correct);
        updateToggleButton(answerWrongBtn, !correct);
        if (answerHintLabel != null) {
            answerHintLabel.setText(correct ? "Cette reponse sera comptee comme correcte." : "Cette reponse sera comptee comme incorrecte.");
        }
    }

    private static void updateToggleButton(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.getStyleClass().remove("btn-rgb");
        button.getStyleClass().remove("btn-rgb-outline");
        button.getStyleClass().add(active ? "btn-rgb" : "btn-rgb-outline");
    }

    private ExamQuestion currentQuestion() {
        return questionsTable == null ? editingQuestion : questionsTable.getSelectionModel().getSelectedItem();
    }

    private ExamAnswer currentAnswer() {
        return answersTable == null ? editingAnswer : answersTable.getSelectionModel().getSelectedItem();
    }

    private void selectQuestionById(int questionId) {
        for (ExamQuestion question : questions) {
            if (question.getId() == questionId) {
                questionsTable.getSelectionModel().select(question);
                questionsTable.scrollTo(question);
                return;
            }
        }
    }

    private void selectAnswerById(int answerId) {
        for (ExamAnswer answer : answers) {
            if (answer.getId() == answerId) {
                answersTable.getSelectionModel().select(answer);
                answersTable.scrollTo(answer);
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

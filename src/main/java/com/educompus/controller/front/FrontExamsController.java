package com.educompus.controller.front;

import com.educompus.model.ExamAnswer;
import com.educompus.model.ExamCatalogueItem;
import com.educompus.model.ExamQuestion;
import com.educompus.repository.ExamRepository;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class FrontExamsController {
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Label totalCoursesLabel;
    @FXML
    private Label totalExamsLabel;
    @FXML
    private Label totalQuestionsLabel;
    @FXML
    private Label sourceLabel;
    @FXML
    private StackPane viewStack;
    @FXML
    private VBox cataloguePane;
    @FXML
    private VBox detailPane;
    @FXML
    private VBox quizPane;
    @FXML
    private FlowPane catalogueFlow;
    @FXML
    private Label selectedCourseLabel;
    @FXML
    private Label selectedExamLabel;
    @FXML
    private Label selectedMetaLabel;
    @FXML
    private Label selectedDescLabel;
    @FXML
    private Label selectedLevelChip;
    @FXML
    private Label selectedDomainChip;
    @FXML
    private Label selectedQuestionChip;
    @FXML
    private Label selectedDurationChip;
    @FXML
    private Button startExamButton;
    @FXML
    private Button downloadCertButton;
    @FXML
    private Label quizTitleLabel;
    @FXML
    private Label quizProgressLabel;
    @FXML
    private Label questionTextLabel;
    @FXML
    private VBox answersBox;
    @FXML
    private Label quizResultLabel;
    @FXML
    private Button prevQuestionButton;
    @FXML
    private Button nextQuestionButton;
    @FXML
    private Button submitQuizButton;
    

    private final ExamRepository repository = new ExamRepository();
    private final List<ExamCatalogueItem> catalogue = new ArrayList<>();

    private ExamCatalogueItem selectedItem;
    private Node selectedCard;
    private List<ExamQuestion> activeQuestions = List.of();
    private int[] selectedAnswers = new int[0];
    private int questionIndex = 0;

    @FXML
    private void initialize() {
        // Reload when repository signals changes (add/update/delete/publish)
        try {
            com.educompus.repository.ExamRepository.CHANGE_COUNTER.addListener((obs, oldV, newV) -> reloadCatalogue());
        } catch (Exception ignored) {
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> reloadCatalogue());
        }
        if (sortCombo != null) {
            sortCombo.getItems().setAll("Cours A-Z", "Examen A-Z", "Questions desc");
            sortCombo.setValue("Cours A-Z");
            sortCombo.valueProperty().addListener((obs, oldValue, newValue) -> applySortAndRender());
        }
        if (catalogueFlow != null && viewStack != null) {
            catalogueFlow.prefWrapLengthProperty().bind(viewStack.widthProperty().subtract(140));
        }
        showPane(cataloguePane);
        reloadCatalogue();
    }

    @FXML
    private void reloadCatalogue() {
        catalogue.clear();
        catalogue.addAll(repository.listCatalogue(searchField == null ? "" : searchField.getText()));
        updateStats();
        applySortAndRender();
        if (catalogue.isEmpty()) {
            selectedItem = null;
            showSelection(null);
            return;
        }
        if (selectedItem == null || catalogue.stream().noneMatch(item -> item.getExamId() == selectedItem.getExamId())) {
            selectedItem = catalogue.get(0);
        }
        showSelection(selectedItem);
    }

    private void applySortAndRender() {
        String sort = sortCombo == null ? "Cours A-Z" : String.valueOf(sortCombo.getValue());
        if ("Examen A-Z".equalsIgnoreCase(sort)) {
            catalogue.sort(Comparator.comparing((ExamCatalogueItem item) -> safe(item.getExamTitle()), String.CASE_INSENSITIVE_ORDER));
        } else if ("Questions desc".equalsIgnoreCase(sort)) {
            catalogue.sort(Comparator.comparingInt(ExamCatalogueItem::getQuestionCount).reversed()
                    .thenComparing(item -> safe(item.getCourseTitle()), String.CASE_INSENSITIVE_ORDER));
        } else {
            catalogue.sort(Comparator.comparing((ExamCatalogueItem item) -> safe(item.getCourseTitle()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(item -> safe(item.getExamTitle()), String.CASE_INSENSITIVE_ORDER));
        }
        renderCatalogue();
    }

    private void updateStats() {
        Set<Integer> courseIds = new LinkedHashSet<>();
        int questionsCount = 0;
        for (ExamCatalogueItem item : catalogue) {
            courseIds.add(item.getCourseId());
            questionsCount += item.getQuestionCount();
        }
        totalCoursesLabel.setText(String.valueOf(courseIds.size()));
        totalExamsLabel.setText(String.valueOf(catalogue.size()));
        totalQuestionsLabel.setText(String.valueOf(questionsCount));
        if (sourceLabel != null) {
            String source = safe(repository.getSourceLabel());
            sourceLabel.setText(source);
            sourceLabel.setManaged(!source.isBlank());
            sourceLabel.setVisible(!source.isBlank());
        }
    }

    private void renderCatalogue() {
        if (catalogueFlow == null) {
            return;
        }
        catalogueFlow.getChildren().clear();
        selectedCard = null;
        for (ExamCatalogueItem item : catalogue) {
            VBox card = buildCatalogueCard(item);
            catalogueFlow.getChildren().add(card);
            if (selectedItem != null && selectedItem.getExamId() == item.getExamId()) {
                selectedCard = card;
                card.getStyleClass().add("selected");
            }
        }
    }

    private VBox buildCatalogueCard(ExamCatalogueItem item) {
        Label courseTitle = new Label(item.getCourseTitle());
        courseTitle.getStyleClass().add("exam-card-title");
        courseTitle.setWrapText(true);

        Label examTitle = new Label(item.getExamTitle());
        examTitle.getStyleClass().add("exam-card-subtitle");
        examTitle.setWrapText(true);

        Label desc = new Label(summarize(item.getExamDescription().isBlank() ? item.getCourseDescription() : item.getExamDescription(), 140));
        desc.getStyleClass().add("page-subtitle");
        desc.setWrapText(true);

        HBox chips = new HBox(8,
                chip(item.getLevelLabel(), "chip chip-info"),
                chip(item.getDomainLabel(), "chip chip-outline"),
                chip(item.getQuestionCount() + " questions", "chip chip-warning"));

        Button openBtn = new Button("Voir examen");
        openBtn.getStyleClass().add("btn-rgb");
        openBtn.setOnAction(event -> {
            selectItem(item, cardOf(openBtn));
            openSelectedDetail();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(10, chip(item.getEstimatedMinutes() + " min", "chip chip-success"), spacer, openBtn);
        footer.getStyleClass().add("exam-card-footer");

        VBox card = new VBox(10, courseTitle, examTitle, chips, desc, footer);
        card.getStyleClass().addAll("card", "exam-course-card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(280);
        card.setMinHeight(220);
        card.setOnMouseClicked(event -> selectItem(item, card));
        return card;
    }

    @FXML
    private void backToCatalogue() {
        showPane(cataloguePane);
    }

    private void openSelectedDetail() {
        if (selectedItem == null) {
            info("Examens", "Selectionnez un examen.");
            return;
        }
        showSelection(selectedItem);
        showPane(detailPane);
    }

    @FXML
    private void backToDetail() {
        showPane(detailPane);
    }

    @FXML
    private void startSelectedExam() {
        if (selectedItem == null) {
            info("Examens", "Selectionnez un examen.");
            return;
        }
        activeQuestions = repository.listQuestionsByExamId(selectedItem.getExamId());
        if (activeQuestions.isEmpty()) {
            info("Examens", "Aucune question n'est disponible pour ce quiz.");
            return;
        }
        selectedAnswers = new int[activeQuestions.size()];
        java.util.Arrays.fill(selectedAnswers, -1);
        questionIndex = 0;
        quizResultLabel.setText("Selectionnez une reponse pour continuer.");
        renderQuestion();
        showPane(quizPane);
    }

    @FXML
    private void previousQuestion() {
        if (questionIndex > 0) {
            questionIndex--;
            renderQuestion();
        }
    }

    @FXML
    private void nextQuestion() {
        if (!captureCurrentAnswer(true)) {
            // diagnostic: list answer nodes and selection state
            StringBuilder dbg = new StringBuilder();
            dbg.append("Answers children: ").append(answersBox.getChildren().size()).append("\n");
            int idx = 0;
            for (javafx.scene.Node node : answersBox.getChildren()) {
                dbg.append(idx++).append(": ").append(node.getClass().getSimpleName());
                if (node instanceof RadioButton rb) {
                    dbg.append(" selected=").append(rb.isSelected());
                    Object ud = rb.getUserData();
                    dbg.append(" userData=").append(ud == null ? "null" : ud.toString());
                }
                dbg.append("\n");
            }
            info("Quiz - Selection manquante", dbg.toString());
            return;
        }
        if (questionIndex < activeQuestions.size() - 1) {
            questionIndex++;
            renderQuestion();
        }
    }

    @FXML
    private void submitQuiz() {
        if (!captureCurrentAnswer(true)) {
            return;
        }
        int score = 0;
        for (int i = 0; i < activeQuestions.size(); i++) {
            int selectedIndex = selectedAnswers[i];
            if (selectedIndex >= 0 && selectedIndex < activeQuestions.get(i).getAnswers().size()
                    && activeQuestions.get(i).getAnswers().get(selectedIndex).isCorrect()) {
                score++;
            }
        }
        int total = activeQuestions.size();
        int percent = total == 0 ? 0 : (int) Math.round((score * 100.0) / total);
        quizResultLabel.setText("Resultat: %d/%d bonnes reponses (%d%%).".formatted(score, total, percent));
        nextQuestionButton.setDisable(true);
        submitQuizButton.setDisable(true);
        answersBox.setDisable(true);

        // Attempt logic: max 2 attempts per user per exam
        String email = com.educompus.app.AppState.getUserEmail();
        int uid = com.educompus.app.AppState.getUserId();
        int examId = selectedItem == null ? 0 : selectedItem.getExamId();
        try {
            int prevAttempts = repository.getAttemptCount(email, examId);
            int attemptNumber = prevAttempts + 1;
            if (prevAttempts >= 2) {
                info("Quiz", "Vous avez deja utilise vos 2 essais pour cet examen.");
                return;
            }
            boolean passed = percent >= 50;
            String certificatePath = null;
            if (passed) {
                // generate certificate
                String name = com.educompus.app.AppState.getUserDisplayName();
                certificatePath = repository.createCertificatePdf(name == null ? email : name, email, selectedItem.getExamTitle(), percent, examId);
                repository.recordAttempt(email, examId, percent, true, certificatePath);

                // show dialog with download button
                javafx.scene.control.Alert done = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                done.setTitle("Felicitation");
                done.setHeaderText("Vous avez reussi l'examen !");
                done.setContentText("Score: " + percent + "%\nCliquez sur 'Telecharger' pour recuperer votre certificat.");
                javafx.scene.control.ButtonType download = new javafx.scene.control.ButtonType("Telecharger");
                javafx.scene.control.ButtonType close = javafx.scene.control.ButtonType.OK;
                done.getButtonTypes().setAll(download, close);
                java.util.Optional<javafx.scene.control.ButtonType> res = done.showAndWait();
                if (res.isPresent() && res.get() == download) {
                    try {
                        java.awt.Desktop.getDesktop().open(new java.io.File(certificatePath));
                    } catch (Exception e) {
                        info("Fichier", "Impossible d'ouvrir le certificat: " + certificatePath);
                    }
                }
                return;
            } else {
                // failed
                repository.recordAttempt(email, examId, percent, false, null);
                if (attemptNumber == 1) {
                    javafx.scene.control.Alert ask = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    ask.setTitle("Echec - Essai");
                    ask.setHeaderText("Votre score est " + percent + "%.");
                    ask.setContentText("Voulez-vous retenter l'examen maintenant ? (2 essais maximum)");
                    ask.getButtonTypes().setAll(javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
                    java.util.Optional<javafx.scene.control.ButtonType> r = ask.showAndWait();
                    if (r.isPresent() && r.get() == javafx.scene.control.ButtonType.YES) {
                        // restart quiz
                        startSelectedExam();
                    } else {
                        showPane(detailPane);
                    }
                } else {
                    info("Echec", "Vous avez echoue l'examen. Vous n'avez plus d'essais disponibles.");
                }
            }
        } catch (Exception e) {
            error("Erreur enregistrement tentative", e);
        }
    }

    @FXML
    private void retryQuiz() {
        startSelectedExam();
    }

    private void renderQuestion() {
        if (activeQuestions.isEmpty()) {
            return;
        }
        ExamQuestion question = activeQuestions.get(questionIndex);
        quizTitleLabel.setText(selectedItem == null ? "Quiz" : selectedItem.getExamTitle());
        quizProgressLabel.setText("Question %d/%d".formatted(questionIndex + 1, activeQuestions.size()));
        questionTextLabel.setText(question.getText());
        answersBox.getChildren().clear();

        ToggleGroup toggleGroup = new ToggleGroup();
        List<ExamAnswer> answers = question.getAnswers();
        for (int i = 0; i < answers.size(); i++) {
            ExamAnswer answer = answers.get(i);
            RadioButton option = new RadioButton(answer.getText());
            option.getStyleClass().add("quiz-answer");
            option.setWrapText(true);
            option.setMaxWidth(Double.MAX_VALUE);
            option.setToggleGroup(toggleGroup);
            option.setUserData(i);
            if (selectedAnswers[questionIndex] == i) {
                option.setSelected(true);
            }
            answersBox.getChildren().add(option);
        }

        answersBox.setDisable(false);
        prevQuestionButton.setDisable(questionIndex == 0);
        nextQuestionButton.setDisable(questionIndex >= activeQuestions.size() - 1);
        submitQuizButton.setDisable(questionIndex != activeQuestions.size() - 1);
    }

    private boolean captureCurrentAnswer(boolean showWarning) {
        if (activeQuestions.isEmpty()) {
            return false;
        }
        int selectedIndex = -1;
        for (Node node : answersBox.getChildren()) {
            if (node instanceof RadioButton radio && radio.isSelected()) {
                selectedIndex = (int) radio.getUserData();
                break;
            }
        }
        if (selectedIndex < 0) {
            if (showWarning) {
                info("Quiz", "Choisissez une reponse avant de continuer.");
            }
            return false;
        }
        selectedAnswers[questionIndex] = selectedIndex;
        return true;
    }

    private void selectItem(ExamCatalogueItem item, Node cardNode) {
        selectedItem = item;
        if (selectedCard != null) {
            selectedCard.getStyleClass().remove("selected");
        }
        selectedCard = cardNode;
        if (selectedCard != null && !selectedCard.getStyleClass().contains("selected")) {
            selectedCard.getStyleClass().add("selected");
        }
        showSelection(item);
    }

    private void showSelection(ExamCatalogueItem item) {
        if (item == null) {
            selectedCourseLabel.setText("Aucun cours trouve");
            selectedExamLabel.setText("Catalogue vide");
            selectedMetaLabel.setText("Ajoutez des examens ou verifiez la connexion a la base.");
            selectedDescLabel.setText("La fiche examen s'affichera ici.");
            selectedLevelChip.setText("Niveau");
            selectedDomainChip.setText("Domaine");
            selectedQuestionChip.setText("0 question");
            selectedDurationChip.setText("0 min");
            startExamButton.setDisable(true);
            return;
        }

        selectedCourseLabel.setText(item.getCourseTitle());
        selectedExamLabel.setText(item.getExamTitle());
        selectedMetaLabel.setText("Cours #" + item.getCourseId() + "  |  " + (item.isPublished() ? "Examen publie" : "Examen non publie"));
        selectedDescLabel.setText(item.getExamDescription().isBlank() ? item.getCourseDescription() : item.getExamDescription());
        selectedLevelChip.setText(item.getLevelLabel());
        selectedDomainChip.setText(item.getDomainLabel());
        selectedQuestionChip.setText(item.getQuestionCount() + " questions");
        selectedDurationChip.setText(item.getEstimatedMinutes() + " min");
        // determine if user already passed
        String email = com.educompus.app.AppState.getUserEmail();
        boolean passed = repository.hasPassed(email, item.getExamId());
        String certPath = repository.getCertificatePath(email, item.getExamId());
        startExamButton.setDisable(passed);
        if (downloadCertButton != null) {
            boolean show = passed || (certPath != null && !certPath.isBlank());
            downloadCertButton.setVisible(show);
            downloadCertButton.setManaged(show);
        }
    }

    @FXML
    private void downloadCertificate() {
        if (selectedItem == null) return;
        String email = com.educompus.app.AppState.getUserEmail();
        String path = repository.getCertificatePath(email, selectedItem.getExamId());
        if (path == null || path.isBlank()) {
            info("Certificat", "Aucun certificat disponible.");
            return;
        }
        try {
            java.awt.Desktop.getDesktop().open(new java.io.File(path));
        } catch (Exception e) {
            error("Certificat", e);
        }
    }

    private void showPane(VBox pane) {
        setPaneVisible(cataloguePane, pane == cataloguePane);
        setPaneVisible(detailPane, pane == detailPane);
        setPaneVisible(quizPane, pane == quizPane);
    }

    private static void setPaneVisible(VBox pane, boolean visible) {
        if (pane == null) {
            return;
        }
        pane.setVisible(visible);
        pane.setManaged(visible);
    }

    private static VBox cardOf(Node node) {
        return node == null ? null : (VBox) node.getParent().getParent();
    }

    private static Label chip(String text, String styles) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styles.split(" "));
        return label;
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
        alert.showAndWait();
    }

    private static void error(String title, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(e == null ? "Erreur" : String.valueOf(e.getMessage()));
        alert.showAndWait();
        if (e != null) e.printStackTrace();
    }
}

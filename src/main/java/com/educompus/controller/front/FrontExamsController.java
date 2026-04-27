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
import javafx.geometry.Pos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.beans.value.ChangeListener;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import java.time.Instant;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.Enumeration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.awt.image.BufferedImage;
import com.educompus.app.AppState;
import com.educompus.util.Dialogs;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

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
    private Label quizTimerLabel;
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
    // Activity tracking for cheating detection
    private Stage primaryStage;
    private final List<String> activityLog = new ArrayList<>();
    private ChangeListener<Boolean> stageFocusListener;
    private javafx.event.EventHandler<KeyEvent> keyEventHandler;
    private ScheduledExecutorService clipboardPoller;
    private String lastClipboardString = "";
    private volatile boolean trackingActive = false;
    private volatile boolean cheatingDetected = false;
    private ChangeListener<Boolean> fullScreenListener;

    // per-question UI timer
    private ScheduledExecutorService questionTimerExecutor;
    private ScheduledFuture<?> questionTimerFuture;
    private volatile int questionRemainingSeconds = 0;

    private synchronized void logActivity(String event) {
        String ts = Instant.now().toString();
        activityLog.add(ts + " " + event);
        System.out.println("[ACTIVITY] " + ts + " " + event);
    }

    private void startActivityTracking(int examId) {
        if (trackingActive) return;
        try {
            if (viewStack == null || viewStack.getScene() == null) return;
            primaryStage = (Stage) viewStack.getScene().getWindow();
            if (primaryStage != null) {
                try { primaryStage.setFullScreen(true); } catch (Exception e) { try { primaryStage.setMaximized(true); } catch (Exception ignored) {} }
                stageFocusListener = (obs, oldV, newV) -> {
                    if (Boolean.FALSE.equals(newV)) {
                        logActivity("window-lost-focus");
                        handleCheatingDetectedInternal(examId, "focus-lost");
                    } else {
                        logActivity("window-gained-focus");
                    }
                };
                primaryStage.focusedProperty().addListener(stageFocusListener);
                fullScreenListener = (obs, oldV, newV) -> {
                    if (Boolean.FALSE.equals(newV) && trackingActive && !cheatingDetected) {
                        logActivity("fullscreen-exited");
                        handleCheatingDetectedInternal(examId, "fullscreen-exit");
                    }
                };
                primaryStage.fullScreenProperty().addListener(fullScreenListener);
            }
            javafx.scene.Scene scene = viewStack.getScene();
            keyEventHandler = ev -> {
                try {
                    if (ev.getCode() == KeyCode.ESCAPE) {
                        logActivity("key-escape");
                        handleCheatingDetectedInternal(examId, "escape-pressed");
                        return;
                    }
                    boolean copy = (ev.isControlDown() || ev.isShortcutDown()) && ev.getCode() == KeyCode.C;
                    boolean paste = (ev.isControlDown() || ev.isShortcutDown()) && ev.getCode() == KeyCode.V;
                    if (copy) logActivity("key-copy");
                    if (paste) logActivity("key-paste");
                } catch (Exception ignored) {}
            };
            scene.addEventFilter(KeyEvent.KEY_PRESSED, keyEventHandler);
            lastClipboardString = javafx.scene.input.Clipboard.getSystemClipboard().getString();
            clipboardPoller = Executors.newSingleThreadScheduledExecutor();
            clipboardPoller.scheduleAtFixedRate(() -> {
                try {
                    String cur = javafx.scene.input.Clipboard.getSystemClipboard().getString();
                    if (cur == null) cur = "";
                    if (!cur.equals(lastClipboardString) && !cur.isEmpty()) {
                        lastClipboardString = cur;
                        logActivity("clipboard-changed");
                    }
                } catch (Exception ignored) {}
            }, 1, 1, TimeUnit.SECONDS);
            trackingActive = true;
            logActivity("tracking-started exam=" + examId);
        } catch (Exception e) {
            System.out.println("[DEBUG] startActivityTracking failed: " + e.getMessage());
        }
    }

    private void stopActivityTracking(int examId, int percent) {
        if (!trackingActive && !cheatingDetected) return;
        // mark inactive early to avoid programmatic fullscreen changes being treated as cheating
        trackingActive = false;
        try {
            if (primaryStage != null) {
                try { if (fullScreenListener != null) primaryStage.fullScreenProperty().removeListener(fullScreenListener); } catch (Exception ignored) {}
                try { primaryStage.setFullScreen(false); } catch (Exception ignored) {}
                try { primaryStage.setMaximized(false); } catch (Exception ignored) {}
                if (stageFocusListener != null) primaryStage.focusedProperty().removeListener(stageFocusListener);
            }
            if (viewStack != null && viewStack.getScene() != null && keyEventHandler != null) {
                viewStack.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, keyEventHandler);
            }
            if (clipboardPoller != null) {
                clipboardPoller.shutdownNow();
                clipboardPoller = null;
            }
            logActivity("tracking-stopped score=" + percent);
            try {
                Path dir = Paths.get(System.getProperty("user.dir"), "var", "activity_logs");
                Files.createDirectories(dir);
                int uid = com.educompus.app.AppState.getUserId();
                String fname = "exam_" + examId + "_user_" + uid + "_" + System.currentTimeMillis() + ".log";
                Path file = dir.resolve(fname);
                Files.write(file, activityLog, java.nio.charset.StandardCharsets.UTF_8);
                logActivity("activity-saved " + file.toString());
                String endpoint = System.getenv("BEHAVIOR_API_URL");
                String apiKey = System.getenv("BEHAVIOR_API_KEY");
                if (endpoint != null && !endpoint.isBlank()) {
                    Executors.newSingleThreadExecutor().submit(() -> {
                        try {
                            com.educompus.service.BehaviorClient.sendReport(endpoint, apiKey, examId, uid, activityLog);
                            System.out.println("[ACTIVITY] sent report to " + endpoint);
                        } catch (Exception ex) {
                            System.out.println("[ACTIVITY] send report failed: " + ex.getMessage());
                        }
                    });
                }
            } catch (IOException io) {
                System.out.println("[ACTIVITY] failed to write activity log: " + io.getMessage());
            }
        } catch (Exception e) {
            System.out.println("[ACTIVITY] stopActivityTracking failed: " + e.getMessage());
        } finally {
            activityLog.clear();
            // ensure per-question timer stopped when activity tracking stops
            try { stopQuestionTimer(); } catch (Exception ignored) {}
        }
    }

    private void handleCheatingDetectedInternal(int examId, String reason) {
        if (cheatingDetected) return;
        cheatingDetected = true;
        logActivity("cheating-detected " + reason);
        try { stopActivityTracking(examId, 0); } catch (Exception ignored) {}
        Platform.runLater(() -> {
            try {
                String email = com.educompus.app.AppState.getUserEmail();
                try { repository.recordAttempt(email, examId, 0, false, null); } catch (Exception ignored) {}
                Alert a = new Alert(Alert.AlertType.WARNING);
                a.setTitle("Triche détectée");
                a.setHeaderText("Essai de triche détecté");
                a.setContentText("Essai de triche détecté: " + reason + "\nL'examen est terminé.");
                try { com.educompus.util.Dialogs.style(a); } catch (Exception ignored) {}
                a.showAndWait();
                showPane(detailPane);
            } catch (Exception e) {
                System.out.println("[ACTIVITY] handleCheatingDetected failed: " + e.getMessage());
            }
        });
    }
    

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
        // QR button near 'Voir examen'
        Button qrBtn = new Button("QR");
        qrBtn.getStyleClass().addAll("btn-rgb-outline", "small");
        qrBtn.setOnAction(e -> {
            try {
                showQrDialog(item);
            } catch (Exception ex) {
                error("QR code", ex instanceof Exception ? (Exception) ex : new Exception(ex.toString()));
            }
        });
        HBox footer = new HBox(10, chip(item.getEstimatedDurationLabel(), "chip chip-success"), spacer, qrBtn, openBtn);
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
            info("Examens", "Sélectionnez un examen.");
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
        quizResultLabel.setText("Sélectionnez une réponse pour continuer.");
        renderQuestion();
        showPane(quizPane);
        try {
            startActivityTracking(selectedItem == null ? 0 : selectedItem.getExamId());
        } catch (Exception ignored) {}
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
        try { stopActivityTracking(selectedItem == null ? 0 : selectedItem.getExamId(), percent); } catch (Exception ignored) {}
        quizResultLabel.setText("Résultat: %d/%d bonnes réponses (%d%%).".formatted(score, total, percent));
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
                info("Quiz", "Vous avez déjà utilisé vos 2 essais pour cet examen.");
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
                    done.setTitle("Félicitations");
                    done.setHeaderText("Vous avez réussi l'examen !");
                    done.setContentText("Score: " + percent + "%\nCliquez sur 'Télécharger' pour récupérer votre certificat.");
                    javafx.scene.control.ButtonType download = new javafx.scene.control.ButtonType("Télécharger");
                    javafx.scene.control.ButtonType close = javafx.scene.control.ButtonType.OK;
                    done.getButtonTypes().setAll(download, close);
                    com.educompus.util.Dialogs.style(done);
                    java.util.Optional<javafx.scene.control.ButtonType> res = done.showAndWait();
                    if (res.isPresent() && res.get() == download) {
                        try {
                            java.awt.Desktop.getDesktop().open(new java.io.File(certificatePath));
                        } catch (Exception e) {
                            info("Fichier", "Impossible d'ouvrir le certificat: " + certificatePath);
                        }
                    }

                    // refresh UI so the detail view reflects the new passed/certificate state
                    try {
                        showSelection(selectedItem);
                    } catch (Exception ignored) {}
                    showPane(detailPane);
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
                    com.educompus.util.Dialogs.style(ask);
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
        // start per-question timer (mm:ss)
        try { startQuestionTimer(); } catch (Exception ignored) {}
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
                info("Quiz", "Choisissez une réponse avant de continuer.");
            }
            return false;
        }
        selectedAnswers[questionIndex] = selectedIndex;
        return true;
    }

    // Timer helpers for per-question countdown
    private void startQuestionTimer() {
        stopQuestionTimer();
        if (activeQuestions == null || activeQuestions.isEmpty()) return;
        int dur = activeQuestions.get(questionIndex).getDurationSeconds();
        if (dur <= 0) dur = 45; // default
        questionRemainingSeconds = dur;
        updateTimerLabel(questionRemainingSeconds);
        questionTimerExecutor = Executors.newSingleThreadScheduledExecutor();
        questionTimerFuture = questionTimerExecutor.scheduleAtFixedRate(() -> {
            try {
                questionRemainingSeconds = Math.max(0, questionRemainingSeconds - 1);
                int rem = questionRemainingSeconds;
                Platform.runLater(() -> updateTimerLabel(rem));
                if (rem <= 0) {
                    stopQuestionTimer();
                    Platform.runLater(() -> {
                        if (questionIndex < activeQuestions.size() - 1) {
                            questionIndex++;
                            renderQuestion();
                        } else {
                            submitQuiz();
                        }
                    });
                }
            } catch (Exception ignored) {}
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void stopQuestionTimer() {
        try {
            if (questionTimerFuture != null) {
                questionTimerFuture.cancel(true);
                questionTimerFuture = null;
            }
        } catch (Exception ignored) {}
        try {
            if (questionTimerExecutor != null) {
                questionTimerExecutor.shutdownNow();
                questionTimerExecutor = null;
            }
        } catch (Exception ignored) {}
        questionRemainingSeconds = 0;
        if (quizTimerLabel != null) Platform.runLater(() -> quizTimerLabel.setText("00:00"));
    }

    private void updateTimerLabel(int seconds) {
        if (quizTimerLabel == null) return;
        int s = Math.max(0, seconds);
        int mm = s / 60;
        int ss = s % 60;
        quizTimerLabel.setText(String.format("%d:%02d", mm, ss));
    }

    // Try to find a LAN IPv4 address we can use instead of localhost
    private static String findLocalLanAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                try {
                    if (!intf.isUp() || intf.isLoopback() || intf.isVirtual()) continue;
                } catch (Exception ignored) {}
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
            // fallback to InetAddress.getLocalHost()
            InetAddress local = InetAddress.getLocalHost();
            if (local instanceof Inet4Address && !local.isLoopbackAddress()) return local.getHostAddress();
        } catch (Exception ignored) {}
        return null;
    }

    // Simple TCP connect probe (non-blocking-ish with timeout)
    private static boolean isHostReachable(String host, int port, int timeoutMs) {
        if (host == null || host.isBlank()) return false;
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), Math.max(200, timeoutMs));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Show QR dialog for an exam
    private void showQrDialog(ExamCatalogueItem item) throws Exception {
        if (item == null) return;
        String base = AppState.getServerBaseUrl();
        if (base == null || base.isBlank()) base = "http://localhost:8000";
        // If the configured base uses localhost or loopback, try to replace with LAN IP so mobile can reach it
        String usedBase = base;
        try {
            URI u = new URI(base);
            String host = u.getHost();
            if (host == null) {
                // fallback: replace literal localhost occurrence
                if (base.contains("localhost") || base.contains("127.0.0.1") || base.contains("0.0.0.0")) {
                    String ip = findLocalLanAddress();
                    if (ip != null && !ip.isBlank()) usedBase = base.replace("localhost", ip).replace("127.0.0.1", ip).replace("0.0.0.0", ip);
                }
            } else if ("localhost".equals(host) || "127.0.0.1".equals(host) || "0.0.0.0".equals(host)) {
                String ip = findLocalLanAddress();
                if (ip != null && !ip.isBlank()) {
                    URI nu = new URI(u.getScheme(), u.getUserInfo(), ip, u.getPort(), u.getPath(), u.getQuery(), u.getFragment());
                    usedBase = nu.toString();
                }
            }
        } catch (Exception ignored) {
            if (base.contains("localhost") || base.contains("127.0.0.1") || base.contains("0.0.0.0")) {
                String ip = findLocalLanAddress();
                if (ip != null && !ip.isBlank()) usedBase = base.replace("localhost", ip).replace("127.0.0.1", ip).replace("0.0.0.0", ip);
            }
        }

        String url = usedBase.endsWith("/") ? usedBase + "exam/take/" + item.getExamId() : usedBase + "/exam/take/" + item.getExamId();
        Image qr = generateQrImage(url, 300);
        if (qr == null) throw new IllegalStateException("QR generation failed");

        ImageView iv = new ImageView(qr);
        iv.setPreserveRatio(true);
        iv.setFitWidth(300);

        Label title = new Label("Ouvrir l'examen sur un appareil mobile");
        title.getStyleClass().add("dialog-title");

        // URL is intentionally hidden in the dialog (copy-only)
        Label subtitle = new Label(url);
        subtitle.getStyleClass().add("page-subtitle");
        subtitle.setWrapText(true);
        subtitle.setVisible(false);
        subtitle.setManaged(false);

        javafx.scene.control.Button copyBtn = new javafx.scene.control.Button("Copier le lien");
        // Use outlined RGB (entouré) style to match app template's gradient border
        copyBtn.getStyleClass().addAll("btn-rgb-outline", "small", "qr-copy-btn");
        copyBtn.setOnAction(ev -> {
            try {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(url);
                Clipboard.getSystemClipboard().setContent(cc);
                info("QR", "Lien copié dans le presse-papiers");
            } catch (Exception ignored) {}
        });

        // App icon for header (optional)
        ImageView appIcon = null;
        try {
            java.net.URL icoUrl = getClass().getResource("/assets/images/app-icon.png");
            if (icoUrl == null) icoUrl = getClass().getResource("/assets/images/app-icon.ico");
            if (icoUrl != null) {
                Image appImg = new Image(icoUrl.toExternalForm(), 24, 24, true, true);
                appIcon = new ImageView(appImg);
                appIcon.getStyleClass().add("qr-dialog-app-icon");
            }
        } catch (Exception ignored) {}

        // Header with title (removed internal close button - window chrome still allows closing)
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header;
        if (appIcon != null) {
            header = new HBox(8, appIcon, title, spacer);
        } else {
            header = new HBox(8, title, spacer);
        }
        header.getStyleClass().add("qr-dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // Wrap QR image with a white frame so it stands out on gradient dialog
        javafx.scene.layout.StackPane ivWrap = new javafx.scene.layout.StackPane(iv);
        ivWrap.getStyleClass().add("qr-image-frame");

        // Connectivity check for the generated URL (help user debug mobile reachability)
        boolean reachable = true;
        try {
            URI checkUri = new URI(usedBase);
            String checkHost = checkUri.getHost();
            int checkPort = checkUri.getPort();
            if (checkPort == -1) checkPort = "https".equalsIgnoreCase(checkUri.getScheme()) ? 443 : 80;
            reachable = isHostReachable(checkHost == null ? "127.0.0.1" : checkHost, checkPort, 700);
        } catch (Exception ignored) { reachable = false; }

        Label warnLabel = new Label();
        warnLabel.getStyleClass().add("qr-warning");
        warnLabel.setWrapText(true);
        // hide and remove from layout when reachable (so no empty gap appears)
        warnLabel.setVisible(!reachable);
        warnLabel.setManaged(!reachable);

        Button forceIpBtn = new Button("Forcer l'IP");
        forceIpBtn.getStyleClass().addAll("btn-rgb-outline", "small");
        forceIpBtn.setVisible(!reachable && findLocalLanAddress() != null);
        forceIpBtn.setManaged(forceIpBtn.isVisible());
        // make a final copy of base for use inside the lambda (lambdas require effectively final variables)
        final String baseForLambda = base;
        forceIpBtn.setOnAction(ev -> {
            try {
                String ip = findLocalLanAddress();
                if (ip == null) return;
                URI original = new URI(baseForLambda);
                URI forced = new URI(original.getScheme(), original.getUserInfo(), ip, original.getPort(), original.getPath(), original.getQuery(), original.getFragment());
                String forcedBase = forced.toString();
                String forcedUrl = forcedBase.endsWith("/") ? forcedBase + "exam/take/" + item.getExamId() : forcedBase + "/exam/take/" + item.getExamId();
                Image newQr = generateQrImage(forcedUrl, 300);
                if (newQr != null) iv.setImage(newQr);
                copyBtn.setOnAction(ev2 -> {
                    try { ClipboardContent cc = new ClipboardContent(); cc.putString(forcedUrl); Clipboard.getSystemClipboard().setContent(cc); info("QR", "Lien copié"); } catch (Exception ignored) {}
                });
                warnLabel.setText("QR régénéré avec l'IP locale " + ip + ". Si le mobile ne peut toujours pas accéder, vérifiez que le serveur écoute sur 0.0.0.0 et que le pare-feu autorise le port.");
                warnLabel.setVisible(true);
                warnLabel.setManaged(true);
            } catch (Exception ignored) {}
        });

        // Center the copy button in its own bar
        HBox copyBar = new HBox(copyBtn);
        copyBar.setAlignment(Pos.CENTER);
        copyBar.getStyleClass().add("qr-dialog-copybar");

        VBox body = new VBox(12, header, ivWrap, warnLabel, forceIpBtn, copyBar);
        // include global root variables so dialog uses the same theme colors
        body.getStyleClass().addAll("root", "qr-dialog-root", "qr-dialog-body");

        Scene scene = new Scene(body);
        try {
            String css = getClass().getResource("/styles/educompus.css").toExternalForm();
            if (css != null) scene.getStylesheets().add(css);
        } catch (Exception ignored) {}

        Stage dialog = new Stage();
        dialog.initOwner(null);
        try { dialog.initOwner((Stage) viewStack.getScene().getWindow()); } catch (Exception ignored) {}
        dialog.initModality(Modality.WINDOW_MODAL);
        // Show the exam title in the window title for clarity
        dialog.setTitle("QR - " + (item.getExamTitle() == null || item.getExamTitle().isBlank() ? "Examen #" + item.getExamId() : item.getExamTitle()));
        dialog.setScene(scene);
        // set application icon for this dialog when available
        try {
            java.net.URL ico = getClass().getResource("/assets/images/app-icon.png");
            if (ico == null) ico = getClass().getResource("/assets/images/app-icon.ico");
            if (ico != null) dialog.getIcons().add(new Image(ico.toExternalForm()));
        } catch (Exception ignored) {}
        dialog.setResizable(false);
        dialog.show();
    }

    private Image generateQrImage(String text, int size) throws WriterException {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text == null ? "" : text, BarcodeFormat.QR_CODE, size, size);
            BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
            return SwingFXUtils.toFXImage(img, null);
        } catch (WriterException we) {
            throw we;
        } catch (Exception e) {
            return null;
        }
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
            selectedDurationChip.setText("0:00");
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
        selectedDurationChip.setText(item.getEstimatedDurationLabel());
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
        // stop activity tracking when leaving the quiz pane
        if (pane != quizPane) {
            try { stopQuestionTimer(); } catch (Exception ignored) {}
            try { stopActivityTracking(selectedItem == null ? 0 : selectedItem.getExamId(), -1); } catch (Exception ignored) {}
        }
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
        try { Dialogs.style(alert); } catch (Exception ignored) {}
        alert.showAndWait();
    }

    private static void error(String title, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(e == null ? "Erreur" : String.valueOf(e.getMessage()));
        try { Dialogs.style(alert); } catch (Exception ignored) {}
        alert.showAndWait();
        if (e != null) e.printStackTrace();
    }
}


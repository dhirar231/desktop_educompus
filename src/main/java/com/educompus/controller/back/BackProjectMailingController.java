package com.educompus.controller.back;

import com.educompus.model.AuthUser;
import com.educompus.model.Project;
import com.educompus.service.ProjectMailingService;
import com.educompus.util.Dialogs;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.net.http.HttpTimeoutException;
import com.educompus.util.OpenAiClient;

public final class BackProjectMailingController {
    @FXML
    private ComboBox<String> mailScopeCombo;

    @FXML
    private ComboBox<Project> projectCombo;

    @FXML
    private VBox selectedStudentsBox;

    @FXML
    private TextField mailSubjectField;

    @FXML
    private TextArea mailMessageArea;

    @FXML
    private Label mailConfigLabel;

    @FXML
    private Label mailStatusLabel;

    @FXML
    private Button sendMailBtn;

    @FXML
    private Button generateAiBtn;
    @FXML
    private Button regenerateAiBtn;

    private final ProjectMailingService mailingService = new ProjectMailingService();
    private final List<AuthUser> students = new ArrayList<>();

    private final Object studentsLock = new Object();
    private PauseTransition subjectDebounce;

    @FXML
    private void initialize() {
        mailScopeCombo.setItems(FXCollections.observableArrayList(
                "Tous les etudiants",
                "Etudiants selectionnes",
                "Soumissionnaires du projet"
        ));
        mailScopeCombo.setValue("Tous les etudiants");
        mailScopeCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshMailScope());

        projectCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Project project) {
                return project == null ? "" : "#" + project.getId() + " - " + safe(project.getTitle());
            }

            @Override
            public Project fromString(String string) {
                return null;
            }
        });

        try {
            students.clear();
            students.addAll(mailingService.listStudents());
            projectCombo.setItems(FXCollections.observableArrayList(mailingService.listProjects()));
            selectedStudentsBox.getChildren().clear();
            for (AuthUser student : students) {
                CheckBox checkBox = new CheckBox(student.displayName() + " (" + student.email() + ")");
                checkBox.getStyleClass().add("mail-student-check");
                checkBox.setUserData(student.id());
                selectedStudentsBox.getChildren().add(checkBox);
            }
            // Listen for project selection changes to refresh submitters list
            projectCombo.valueProperty().addListener((obs, oldP, newP) -> {
                if (currentScope() == ProjectMailingService.Scope.PROJECT_SUBMITTERS) {
                    refreshProjectSubmitters();
                }
            });
            mailConfigLabel.setText(mailingService.getMailConfig().summary());
            refreshMailScope();
            // debounce subject changes to auto-generate message if message area is empty
            subjectDebounce = new PauseTransition(Duration.millis(800));
            mailSubjectField.textProperty().addListener((obs, oldV, newV) -> {
                subjectDebounce.stop();
                if (newV == null || newV.isBlank()) return;
                if (mailMessageArea != null && !safe(mailMessageArea.getText()).isBlank()) return;
                subjectDebounce.setOnFinished(evt -> {
                    // ensure button isn't already disabled
                    if (generateAiBtn != null && !generateAiBtn.isDisabled()) {
                        generateMessageWithAi();
                    }
                });
                subjectDebounce.playFromStart();
            });
        } catch (Exception ex) {
            mailConfigLabel.setText("Chargement impossible: " + safe(ex.getMessage()));
            sendMailBtn.setDisable(true);
        }
    }

    @FXML
    private void toggleStudentsSelection() {
        boolean allSelected = true;
        for (var node : selectedStudentsBox.getChildren()) {
            if (node instanceof CheckBox checkBox && !checkBox.isSelected()) {
                allSelected = false;
                break;
            }
        }
        for (var node : selectedStudentsBox.getChildren()) {
            if (node instanceof CheckBox checkBox) {
                checkBox.setSelected(!allSelected);
            }
        }
    }

    @FXML
    private void sendMailing() {
        ProjectMailingService.Scope scope = currentScope();
        Integer projectId = projectCombo.getValue() == null ? null : projectCombo.getValue().getId();
        List<Integer> selectedIds = selectedStudentIds();
        String subject = text(mailSubjectField);
        String message = text(mailMessageArea);

        sendMailBtn.setDisable(true);
        mailStatusLabel.setText("Envoi en cours...");

        Task<ProjectMailingService.MailingResult> task = new Task<>() {
            @Override
            protected ProjectMailingService.MailingResult call() throws Exception {
                return mailingService.sendMail(scope, projectId, selectedIds, subject, message);
            }
        };

        task.setOnSucceeded(event -> {
            sendMailBtn.setDisable(false);
            ProjectMailingService.MailingResult result = task.getValue();
            StringBuilder status = new StringBuilder("Envoye a ").append(result.sent()).append(" etudiant(s).");
            if (!result.failedEmails().isEmpty()) {
                status.append(" Echec: ");
                boolean first = true;
                for (String email : result.failedEmails()) {
                    if (!first) {
                        status.append(" | ");
                    }
                    String reason = result.failedReasons() == null ? "" : safe(result.failedReasons().get(email));
                    status.append(email);
                    if (!reason.isBlank()) {
                        status.append(" -> ").append(reason);
                    }
                    first = false;
                }
            }
            mailStatusLabel.setText(status.toString());
            Dialogs.info("Mailing", status.toString());
        });

        task.setOnFailed(event -> {
            sendMailBtn.setDisable(false);
            Throwable ex = task.getException();
            String messageText = ex == null ? "Erreur inconnue." : safe(ex.getMessage());
            mailStatusLabel.setText(messageText);
            Dialogs.error("Mailing", messageText);
        });

        Thread thread = new Thread(task, "project-mailing-send");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void generateMessageWithAi() {
        String subject = text(mailSubjectField);
        if (subject.isBlank()) {
            Dialogs.error("Génération IA", "Veuillez saisir le sujet du mail avant de générer.");
            return;
        }

        generateAiBtn.setDisable(true);
        mailStatusLabel.setText("Génération du message en cours...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                try {
                    return OpenAiClient.generateMessageFromSubject(subject);
                } catch (HttpTimeoutException e) {
                    throw new IOException("Temps d'attente dépassé pour l'API OpenAI.", e);
                }
            }
        };

        task.setOnSucceeded(ev -> {
            generateAiBtn.setDisable(false);
            String generated = task.getValue();
            if (generated == null || generated.isBlank()) {
                mailStatusLabel.setText("Aucun contenu généré.");
                Dialogs.error("Génération IA", "Aucun contenu généré par l'IA.");
            } else {
                mailMessageArea.setText(generated);
                mailStatusLabel.setText("Message généré avec succès.");
            }
        });

        task.setOnFailed(ev -> {
            generateAiBtn.setDisable(false);
            Throwable ex = task.getException();
            String msg = ex == null ? "Erreur inconnue." : ex.getMessage();
            mailStatusLabel.setText("Erreur: " + msg);
            Dialogs.error("Génération IA", msg);
        });

        Thread th = new Thread(task, "mailing-ai-generate");
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void regenerateMessageWithAi() {
        String subject = text(mailSubjectField);
        if (subject.isBlank()) {
            Dialogs.error("Génération IA", "Veuillez saisir le sujet du mail avant de régénérer.");
            return;
        }

        regenerateAiBtn.setDisable(true);
        mailStatusLabel.setText("Régénération du message en cours...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                try {
                    return OpenAiClient.generateMessageFromSubject(subject);
                } catch (HttpTimeoutException e) {
                    throw new IOException("Temps d'attente dépassé pour l'API OpenAI.", e);
                }
            }
        };

        task.setOnSucceeded(ev -> {
            regenerateAiBtn.setDisable(false);
            String generated = task.getValue();
            if (generated == null || generated.isBlank()) {
                mailStatusLabel.setText("Aucun contenu généré.");
                Dialogs.error("Génération IA", "Aucun contenu généré par l'IA.");
            } else {
                mailMessageArea.setText(generated);
                mailStatusLabel.setText("Message régénéré avec succès.");
            }
        });

        task.setOnFailed(ev -> {
            regenerateAiBtn.setDisable(false);
            Throwable ex = task.getException();
            String msg = ex == null ? "Erreur inconnue." : ex.getMessage();
            mailStatusLabel.setText("Erreur: " + msg);
            Dialogs.error("Génération IA", msg);
        });

        Thread th = new Thread(task, "mailing-ai-regenerate");
        th.setDaemon(true);
        th.start();
    }

    private void refreshMailScope() {
        ProjectMailingService.Scope scope = currentScope();
        boolean showProject = scope == ProjectMailingService.Scope.PROJECT_SUBMITTERS;
        boolean showSelected = scope == ProjectMailingService.Scope.SELECTED;
        projectCombo.setDisable(!showProject);
        projectCombo.setVisible(showProject);
        projectCombo.setManaged(showProject);
        // Show students box for both SELECTED and PROJECT_SUBMITTERS
        boolean showStudentsBox = showSelected || showProject;
        selectedStudentsBox.setVisible(showStudentsBox);
        selectedStudentsBox.setManaged(showStudentsBox);
        if (showProject) {
            refreshProjectSubmitters();
        } else if (showSelected) {
            // ensure full students list is displayed
            populateStudentsBox(students);
        }
    }

    private void refreshProjectSubmitters() {
        Integer projectId = projectCombo.getValue() == null ? null : projectCombo.getValue().getId();
        // load submitters asynchronously to avoid blocking UI
        Task<java.util.List<AuthUser>> task = new Task<>() {
            @Override
            protected java.util.List<AuthUser> call() throws Exception {
                return mailingService.resolveRecipients(ProjectMailingService.Scope.PROJECT_SUBMITTERS, projectId, null);
            }
        };
        task.setOnSucceeded(ev -> {
            java.util.List<AuthUser> submitters = task.getValue();
            populateStudentsBox(submitters);
        });
        task.setOnFailed(ev -> {
            populateStudentsBox(java.util.List.of());
        });
        Thread t = new Thread(task, "load-project-submitters");
        t.setDaemon(true);
        t.start();
    }

    private void populateStudentsBox(java.util.List<AuthUser> list) {
        synchronized (studentsLock) {
            selectedStudentsBox.getChildren().clear();
            for (AuthUser student : list) {
                CheckBox checkBox = new CheckBox(student.displayName() + " (" + student.email() + ")");
                checkBox.getStyleClass().add("mail-student-check");
                checkBox.setUserData(student.id());
                selectedStudentsBox.getChildren().add(checkBox);
            }
        }
    }

    private ProjectMailingService.Scope currentScope() {
        String value = safe(mailScopeCombo.getValue());
        if (value.startsWith("Etudiants selectionnes")) {
            return ProjectMailingService.Scope.SELECTED;
        }
        if (value.startsWith("Soumissionnaires")) {
            return ProjectMailingService.Scope.PROJECT_SUBMITTERS;
        }
        return ProjectMailingService.Scope.ALL;
    }

    private List<Integer> selectedStudentIds() {
        List<Integer> ids = new ArrayList<>();
        for (var node : selectedStudentsBox.getChildren()) {
            if (node instanceof CheckBox checkBox && checkBox.isSelected()) {
                Object raw = checkBox.getUserData();
                if (raw instanceof Integer value && value > 0) {
                    ids.add(value);
                }
            }
        }
        return ids;
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
}

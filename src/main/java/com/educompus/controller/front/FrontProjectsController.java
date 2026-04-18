package com.educompus.controller.front;

import com.educompus.app.AppState;
import com.educompus.model.KanbanStatus;
import com.educompus.model.KanbanTask;
import com.educompus.model.Project;
import com.educompus.model.ProjectSubmission;
import com.educompus.model.ProjectSubmissionView;
import com.educompus.service.JcefBrowserService;
import com.educompus.service.ProjectMeetingService;
import com.educompus.repository.KanbanTaskRepository;
import com.educompus.repository.NotificationRepository;
import com.educompus.repository.ProjectRepository;
import com.educompus.repository.ProjectSubmissionRepository;
import com.educompus.util.ProjectRules;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class FrontProjectsController {
    private enum KanbanReturn {
        SUBMIT,
        SUBMISSIONS
    }
    
    @FXML
    private TextField projectSearchField;

    @FXML
    private ComboBox<String> projectSortCombo;

    @FXML
    private StackPane viewStack;

    @FXML
    private VBox cataloguePane;

    @FXML
    private VBox viewPane;

    @FXML
    private VBox submitPane;

    @FXML
    private VBox kanbanPane;

    @FXML
    private VBox mySubmissionsPane;

    @FXML
    private FlowPane cardsFlow;

    @FXML
    private Label viewTitleLabel;

    @FXML
    private Label viewMetaLabel;

    @FXML
    private Label viewDescLabel;

    @FXML
    private Label viewMeetingStatusLabel;

    @FXML
    private Label viewMeetingLinkLabel;

    @FXML
    private Button viewMeetingOpenButton;

    @FXML
    private Button viewMeetingMutedButton;

    @FXML
    private Button viewMeetingCopyButton;

    @FXML
    private Label submitTitleLabel;

    @FXML
    private Label submitMetaLabel;

    @FXML
    private Label submitDescLabel;

    @FXML
    private Label selectedProjectLabel;

    @FXML
    private Label submitMeetingStatusLabel;

    @FXML
    private Button submitMeetingOpenButton;

    @FXML
    private Button submitMeetingMutedButton;

    @FXML
    private Button submitMeetingCopyButton;

    @FXML
    private TextArea responseArea;

    @FXML
    private TextField cahierPathField;

    @FXML
    private TextField dossierPathField;

    @FXML
    private Label submitInfoLabel;

    @FXML
    private Button submitButton;

    @FXML
    private Label mySubmissionsTitleLabel;

    @FXML
    private ComboBox<String> submissionsSortCombo;

    @FXML
    private TextField submissionsSearchField;

    @FXML
    private Label kanbanTitleLabel;

    @FXML
    private TextField kanbanSearchField;

    @FXML
    private TextField kanbanTaskTitleField;

    @FXML
    private ComboBox<KanbanStatus> kanbanTaskStatusCombo;

    @FXML
    private TextArea kanbanTaskDescArea;

    @FXML
    private Button kanbanAddButton;

    @FXML
    private VBox kanbanTodoColumn;

    @FXML
    private VBox kanbanInProgressColumn;

    @FXML
    private VBox kanbanDoneColumn;

    @FXML
    private ListView<ProjectSubmissionView> submissionsList;

    @FXML
    private Label selectedSubmissionLabel;

    // repositories used by this controller
    private final ProjectRepository projectRepo = new ProjectRepository();
    private final ProjectSubmissionRepository submissionRepo = new ProjectSubmissionRepository();
    private final NotificationRepository notificationRepo = new NotificationRepository();
    private final KanbanTaskRepository kanbanRepo = new KanbanTaskRepository();
    private final ProjectMeetingService projectMeetingService = new ProjectMeetingService();
    private final JcefBrowserService browserService = JcefBrowserService.getInstance();

    private final ObservableList<Project> projects = FXCollections.observableArrayList();
    private final ObservableList<Project> allProjects = FXCollections.observableArrayList();
    private final ObservableList<ProjectSubmissionView> submissions = FXCollections.observableArrayList();
    private final ObservableList<ProjectSubmissionView> allSubmissions = FXCollections.observableArrayList();
    private final List<KanbanTask> kanbanAllTasks = new ArrayList<>();

    private Project selectedProject;
    private ProjectSubmissionView selectedSubmission;
    private Node selectedProjectCard;

    private int submissionsProjectFilterId = 0;
    private String submissionsProjectFilterTitle = "";

    private int kanbanProjectId = 0;
    private String kanbanProjectTitle = "";
    private KanbanReturn kanbanReturn = KanbanReturn.SUBMIT;

    private int editingSubmissionId = 0;

    @FXML
    private void initialize() {
        setupSubmissionsTable();
        setupKanbanCombos();
        setupKanbanDnD();
        setupSortCombos();
        setupValidation();
        showCatalogue();

        if (projectSearchField != null) {
            projectSearchField.textProperty().addListener((obs, oldV, newV) -> applyProjectFilter());
        }
        if (submissionsSearchField != null) {
            submissionsSearchField.textProperty().addListener((obs, o, n) -> applySubmissionsFilterAndSort());
        }
        if (kanbanSearchField != null) {
            kanbanSearchField.textProperty().addListener((obs, o, n) -> renderKanbanFiltered());
        }

        reloadProjects(null);
        reloadSubmissions(null);
        // make FlowPane wrap length follow the available view width so we can fit 3 cards per row
        if (cardsFlow != null && viewStack != null) {
            cardsFlow.prefWrapLengthProperty().bind(viewStack.widthProperty().subtract(160));
        }
    }

    private void setupSortCombos() {
        if (projectSortCombo != null) {
            projectSortCombo.getItems().setAll("Plus recentes", "Titre A-Z", "Deadline");
            projectSortCombo.setValue("Plus recentes");
            projectSortCombo.valueProperty().addListener((obs, o, n) -> applyProjectFilter());
        }
        if (submissionsSortCombo != null) {
            submissionsSortCombo.getItems().setAll("Date desc", "Date asc", "Projet A-Z");
            submissionsSortCombo.setValue("Date desc");
            submissionsSortCombo.valueProperty().addListener((obs, o, n) -> applySubmissionsFilterAndSort());
        }
    }

    private void setupValidation() {
        if (responseArea != null) {
            responseArea.textProperty().addListener((obs, o, n) -> refreshSubmitValidation());
        }
        if (cahierPathField != null) {
            cahierPathField.textProperty().addListener((obs, o, n) -> refreshSubmitValidation());
        }
        if (dossierPathField != null) {
            dossierPathField.textProperty().addListener((obs, o, n) -> refreshSubmitValidation());
        }
        if (kanbanTaskTitleField != null) {
            // enforce only letters/spaces/ '-' and max 16 chars while typing
            javafx.scene.control.TextFormatter<String> tf = new javafx.scene.control.TextFormatter<>(change -> {
                String newText = change.getControlNewText();
                if (newText.length() > 16) {
                    return null;
                }
                // allow letters, spaces, apostrophe, hyphen
                if (!newText.matches("[\\p{L}\\s'\\-]*")) {
                    return null;
                }
                return change;
            });
            kanbanTaskTitleField.setTextFormatter(tf);
            kanbanTaskTitleField.textProperty().addListener((obs, o, n) -> refreshKanbanValidation());
        }
        refreshKanbanValidation();
        refreshSubmitValidation();
    }

    private void refreshKanbanValidation() {
        if (kanbanAddButton == null) {
            return;
        }
        // do not block typing by disabling the add button for invalid titles;
        // only require a selected project. Validation will be shown when user attempts to add.
        kanbanAddButton.setDisable(kanbanProjectId <= 0);
    }

    private void refreshSubmitValidation() {
        if (submitButton == null) {
            return;
        }
        String response = text(responseArea);
        String cahier = text(cahierPathField);
        String dossier = text(dossierPathField);

        String err = ProjectRules.validateSubmissionFields(response, cahier, dossier);
        submitButton.setDisable(err != null);
        if (submitInfoLabel != null) {
            if (err != null) {
                submitInfoLabel.setText(err);
            } else {
                refreshSubmitInfo();
            }
        }
    }

    private static String validateSubmissionFields(String response, String cahierPath, String dossierPath) {
        String r = safe(response);
        if (r.isBlank()) {
            return "Réponse requise (texte + chiffres).";
        }
        if (!isSubmissionResponseValid(r)) {
            return "Réponse invalide (autorisé: lettres, chiffres, espaces).";
        }

        String c = safe(cahierPath);
        if (c.isBlank()) {
            return "Fichier cahier requis (pdf/doc/pptx/image/... ).";
        }
        if (!isFilePathValid(c, List.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "jpg", "jpeg", "png"))) {
            return "Cahier invalide (type/chemin). Formats acceptés: pdf, doc, docx, ppt, pptx, xls, xlsx, jpg, jpeg, png.";
        }

        String d = safe(dossierPath);
        if (d.isBlank()) {
            return "Fichier dossier requis (.zip/.rar/.7z).";
        }
        if (!isFilePathValid(d, List.of("zip", "rar", "7z"))) {
            return "Dossier invalide (type/chemin).";
        }

        return null;
    }

    private static boolean isSubmissionResponseValid(String text) {
        String v = safe(text);
        if (v.length() < 2) {
            return false;
        }
        return v.matches("[\\p{L}\\p{N}\\s\\.,;:!\\?\\(\\)\\[\\]\\{\\}'\"\\-_/\\\\@#]+");
    }

    private static boolean isKanbanTitleValid(String text) {
        String v = safe(text);
        if (v.length() < 2 || v.length() > 16) {
            return false;
        }
        // must start with a letter and only contain letters, spaces, apostrophe or hyphen
        return v.matches("^[\\p{L}][\\p{L}\\s'\\-]{1,15}$");
    }

    private static String kanbanTitleValidationError(String text) {
        String v = safe(text);
        if (v.isBlank()) {
            return "Titre requis (2–16 lettres).";
        }
        if (v.length() < 2) {
            return "Titre trop court (min 2 lettres).";
        }
        if (v.length() > 16) {
            return "Titre trop long (max 16 caractères).";
        }
        if (!Character.isLetter(v.codePointAt(0))) {
            return "Le titre doit commencer par une lettre.";
        }
        if (!v.matches("^[\\p{L}][\\p{L}\\s'\\-]{1,15}$")) {
            return "Caractères invalides: n'utilisez que des lettres, espaces, apostrophe ou tiret.";
        }
        return null;
    }

    private static boolean isFilePathValid(String path, List<String> allowedExt) {
        String p = safe(path);
        if (p.isBlank()) {
            return false;
        }
        File f = new File(p);
        if (!f.exists() || !f.isFile()) {
            return false;
        }
        String ext = extensionOf(f.getName());
        if (ext.isBlank()) {
            return false;
        }
        for (String a : allowedExt) {
            if (ext.equalsIgnoreCase(safe(a))) {
                return true;
            }
        }
        return false;
    }

        private void setupSubmissionsTable() {
            if (submissionsList == null) {
                return;
            }
            submissionsList.setItems(submissions);
            submissionsList.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(ProjectSubmissionView s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    Label title = new Label(safe(s.getProjectTitle()));
                    title.getStyleClass().add("stat-value");

                    Label meta = new Label(safe(s.getSubmittedAt()));
                    meta.getStyleClass().add("page-subtitle");

                    Label resp = new Label(summarize(safe(s.getTextResponse()), 120));
                    resp.setWrapText(true);

                    Button dlCahier = new Button("Télécharger");
                    dlCahier.getStyleClass().add("btn-rgb-compact");
                    dlCahier.setDisable(safe(s.getCahierPath()).isBlank());
                    dlCahier.setOnAction(e -> downloadFromPath(s.getCahierPath(), "fichier"));

                    Button dlZip = new Button("Télécharger");
                    dlZip.getStyleClass().add("btn-rgb-compact");
                    dlZip.setDisable(safe(s.getDossierPath()).isBlank());
                    dlZip.setOnAction(e -> downloadFromPath(s.getDossierPath(), "zip"));

                    Button edit = new Button("Modifier");
                    edit.getStyleClass().add("btn-rgb-outline");
                    edit.setOnAction(e -> {
                        startEditingSubmission(s);
                        submissionsList.getSelectionModel().select(s);
                        Project p = findProjectById(s.getProjectId());
                        if (p == null) {
                            p = new Project();
                            p.setId(s.getProjectId());
                            p.setTitle(safe(s.getProjectTitle()));
                            p.setDescription("");
                            p.setDeadline("");
                            p.setDeliverables("");
                        }
                        selectedProject = p;
                        showSubmit(p);
                    });

                    Button del = new Button("Supprimer");
                    del.getStyleClass().add("btn-danger");
                    del.setOnAction(e -> deleteSubmissionMine(s));

                    HBox right = new HBox(8, dlCahier, dlZip, edit, del);
                    VBox left = new VBox(4, title, meta, resp);
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    HBox row = new HBox(12, left, spacer, right);
                    row.getStyleClass().add("submission-row");
                    setGraphic(row);
                    setText(null);
                }
            });

            submissionsList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                selectedSubmission = newV;
                updateSelectedSubmissionLabel();
            });
        }

	    private void deleteSubmissionMine(ProjectSubmissionView submission) {
	        if (submission == null) {
	            return;
	        }
	        int uid = AppState.getUserId();
	        if (uid <= 0) {
	            info("Soumission", "Compte non identifie.");
	            return;
	        }
	        boolean ok = confirmRgb("Supprimer", "Confirmer la suppression de votre soumission ?");
	        if (!ok) {
	            return;
	        }
	        try {
	            submissionRepo.deleteMine(submission.getId(), uid);
	            reloadSubmissions(null);
	        } catch (Exception e) {
	            error("Erreur", e);
	        }
	    }

    private void setupKanbanCombos() {
        initStatusCombo(kanbanTaskStatusCombo);
        renderKanbanColumns(List.of());
        // do not restrict typing here; validate on add/update and guide the user with warnings
    }

    private void setupKanbanDnD() {
        setupKanbanDropZone(kanbanTodoColumn, KanbanStatus.TODO);
        setupKanbanDropZone(kanbanInProgressColumn, KanbanStatus.IN_PROGRESS);
        setupKanbanDropZone(kanbanDoneColumn, KanbanStatus.DONE);
    }

    private void setupKanbanDropZone(VBox column, KanbanStatus target) {
        if (column == null) {
            return;
        }
        column.setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (db != null && db.hasString()) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });
        column.setOnDragDropped(e -> {
            boolean ok = false;
            Dragboard db = e.getDragboard();
            if (db != null && db.hasString()) {
                try {
                    int id = Integer.parseInt(db.getString());
                    kanbanRepo.updateStatus(id, target);
	            reloadKanbanPage(null);
	            refreshKanbanValidation();
                    ok = true;
                } catch (Exception ex) {
                    error("Erreur Kanban", ex);
                }
            }
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    private void initStatusCombo(ComboBox<KanbanStatus> combo) {
        if (combo == null) {
            return;
        }
        combo.getItems().setAll(KanbanStatus.values());
        combo.setValue(KanbanStatus.TODO);
        combo.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(KanbanStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
        combo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(KanbanStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
    }

    @FXML
    private void reloadProjects(ActionEvent event) {
        allProjects.setAll(projectRepo.listAll(""));
        applyProjectFilter();
    }

    private void renderProjectCards() {
        if (cardsFlow == null) {
            return;
        }
        cardsFlow.getChildren().clear();

        Node keepSelectedCard = null;
        for (Project p : projects) {
            VBox card = buildProjectCard(p);
            if (selectedProject != null && p.getId() == selectedProject.getId()) {
                keepSelectedCard = card;
                if (!card.getStyleClass().contains("selected")) {
                    card.getStyleClass().add("selected");
                }
            }
            cardsFlow.getChildren().add(card);
        }

        selectedProjectCard = keepSelectedCard;
    }

    private VBox buildProjectCard(Project project) {
        VBox card = new VBox(10);
        card.getStyleClass().add("project-card");
        card.setPadding(new Insets(14));
        card.setPrefWidth(260);
        card.setMinWidth(240);

        HBox top = new HBox(10);
        Label title = new Label(safe(project.getTitle()));
        title.getStyleClass().add("project-card-title");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label chip = new Label(deadlineChipText(project.getDeadline()));
        chip.getStyleClass().addAll("chip", "chip-info");

        top.getChildren().addAll(title, chip);

        String desc = safe(project.getDescription());
        Label subtitle = new Label(desc.isBlank() ? "Aucune description" : summarize(desc, 110));
        subtitle.getStyleClass().add("project-card-subtitle");
        subtitle.setWrapText(true);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);

        Button btnVoir = new Button("Voir");
        btnVoir.getStyleClass().add("btn-rgb-outline");
        btnVoir.setOnAction(e -> {
            setSelectedProject(project, card);
            openView(null);
        });

        Button btnSubmit = new Button("Soumettre");
        btnSubmit.getStyleClass().add("btn-rgb");
        btnSubmit.setOnAction(e -> {
            setSelectedProject(project, card);
            openSubmit(null);
        });

        HBox actions = new HBox(10, grow, btnVoir, btnSubmit);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        card.getChildren().addAll(top, subtitle, actions);
        card.setOnMouseClicked(e -> {
            setSelectedProject(project, card);
            if (e.getClickCount() >= 2) {
                openView(null);
            }
        });
        return card;
    }

    private void setSelectedProject(Project project, Node card) {
        selectedProject = project;
        if (selectedProjectCard != null) {
            selectedProjectCard.getStyleClass().remove("selected");
        }
        selectedProjectCard = card;
        if (selectedProjectCard != null && !selectedProjectCard.getStyleClass().contains("selected")) {
            selectedProjectCard.getStyleClass().add("selected");
        }
        updateTopButtonsState();
    }

    private void updateTopButtonsState() {
        // no-op (buttons are per-card)
    }

    private void applyProjectFilter() {
        String q = projectSearchField == null ? "" : String.valueOf(projectSearchField.getText());
        String query = safe(q).toLowerCase();
        if (query.isBlank()) {
            // show only published projects in front catalogue
            List<Project> published = new ArrayList<>();
            for (Project p : allProjects) {
                if (p != null && p.isPublished()) published.add(p);
            }
            projects.setAll(published);
	        } else {
	            List<Project> filtered = new ArrayList<>();
	            for (Project p : allProjects) {
	                String t = safe(p.getTitle()).toLowerCase();
	                String d = safe(p.getDescription()).toLowerCase();
                if ((t.contains(query) || d.contains(query)) && p.isPublished()) {
	                    filtered.add(p);
	                }
	            }
	            projects.setAll(filtered);
	        }

	        String sort = projectSortCombo == null ? "" : safe(projectSortCombo.getValue());
            if ("Titre A-Z".equalsIgnoreCase(sort)) {
                projects.sort(Comparator.comparing((Project p) -> safe(p == null ? null : p.getTitle()).toLowerCase()));
            } else if ("Deadline".equalsIgnoreCase(sort)) {
                projects.sort(Comparator.comparing((Project p) -> safe(p == null ? null : p.getDeadline())));
            } else {
                projects.sort(Comparator.comparing((Project p) -> safe(p == null ? null : p.getCreatedAt())).reversed());
            }

        if (selectedProject != null) {
            boolean stillThere = false;
            for (Project p : projects) {
                if (p.getId() == selectedProject.getId()) {
                    stillThere = true;
                    break;
                }
            }
            if (!stillThere) {
                selectedProject = null;
                selectedProjectCard = null;
            }
        }

        if (selectedProject == null && !projects.isEmpty()) {
            setSelectedProject(projects.get(0), null);
        }

        renderProjectCards();
        updateTopButtonsState();
    }

    @FXML
    private void openSelectedProject(ActionEvent event) {
        openView(event);
    }

    @FXML
    private void openMySubmissions(ActionEvent event) {
        if (selectedProject != null) {
            openMySubmissionsForProject(selectedProject);
            return;
        }
        clearSubmissionsProjectFilter();
        showMySubmissions();
        reloadSubmissions(null);
    }

    @FXML
    private void backToCatalog(ActionEvent event) {
        clearSubmissionsProjectFilter();
        stopEditingAndResetForm();
        showCatalogue();
    }

    private void showCatalogue() {
        setView(cataloguePane);
        setTopControlsVisible(true);
    }

    private void setTopControlsVisible(boolean on) {
        if (projectSearchField != null) {
            projectSearchField.setVisible(on);
            projectSearchField.setManaged(on);
        }
        if (projectSortCombo != null) {
            projectSortCombo.setVisible(on);
            projectSortCombo.setManaged(on);
        }
    }

    private void showView(Project project) {
        if (project == null) {
            return;
        }
        project = refreshProjectState(project);
        setView(viewPane);
        setTopControlsVisible(true);
        fillProjectLabels(project, viewTitleLabel, viewMetaLabel, viewDescLabel);
        updateMeetingPanel(project);
    }

    private void showSubmit(Project project) {
        if (project == null) {
            return;
        }
        project = refreshProjectState(project);
        setView(submitPane);
        setTopControlsVisible(true);
        fillProjectLabels(project, submitTitleLabel, submitMetaLabel, submitDescLabel);

	        if (selectedProjectLabel != null) {
	            selectedProjectLabel.setText("Projet: " + safe(project.getTitle()));
	        }

            updateMeetingPanel(project);

		        refreshSubmitInfo();
		        updateSubmitButtonLabel();
		        refreshSubmitValidation();
		    }

    private void showMySubmissions() {
        setView(mySubmissionsPane);
        // hide top search/sort controls on this page
        setTopControlsVisible(false);
    }

    @FXML
    private void openView(ActionEvent event) {
        if (selectedProject == null) {
            info("Projet", "Sélectionnez un projet dans le catalogue.");
            return;
        }
        showView(selectedProject);
    }

    @FXML
    private void openSubmit(ActionEvent event) {
        if (selectedProject == null) {
            info("Projet", "Sélectionnez un projet dans le catalogue.");
            return;
        }
        stopEditingAndResetForm();
        showSubmit(selectedProject);
    }

    @FXML
    private void goToSubmit(ActionEvent event) {
        openSubmit(event);
    }

    @FXML
    private void openKanbanFromSubmit(ActionEvent event) {
        if (selectedProject == null) {
            info("Kanban", "Sélectionnez un projet.");
            return;
        }
        int uid = AppState.getUserId();
        String mail = AppState.getUserEmail();
        if (uid <= 0) {
            info("Kanban", "Compte non identifié (userId manquant).");
            return;
        }
        if (!submissionRepo.hasSubmissionMine(selectedProject.getId(), uid, mail)) {
            info("Kanban", "Soumettez d'abord le projet, puis ouvrez le tableau Kanban.");
            return;
        }
        openKanban(selectedProject.getId(), selectedProject.getTitle(), KanbanReturn.SUBMIT);
    }

    @FXML
    private void openKanbanForSelectedSubmission(ActionEvent event) {
        if (selectedSubmission == null) {
            info("Kanban", "Sélectionnez une soumission.");
            return;
        }
        openKanban(selectedSubmission.getProjectId(), selectedSubmission.getProjectTitle(), KanbanReturn.SUBMISSIONS);
    }

    @FXML
    private void editSelectedSubmission(ActionEvent event) {
        if (selectedSubmission == null) {
            info("Soumission", "Sélectionnez une soumission à modifier.");
            return;
        }
        Project p = findProjectById(selectedSubmission.getProjectId());
        if (p == null) {
            Project fallback = new Project();
            fallback.setId(selectedSubmission.getProjectId());
            fallback.setTitle(safe(selectedSubmission.getProjectTitle()));
            fallback.setDescription("");
            fallback.setDeadline("");
            fallback.setDeliverables("");
            p = fallback;
        }
        selectedProject = p;
        startEditingSubmission(selectedSubmission);
        showSubmit(p);
    }

    @FXML
    private void backFromKanban(ActionEvent event) {
        if (kanbanReturn == KanbanReturn.SUBMISSIONS) {
            showMySubmissions();
        } else {
            showSubmit(selectedProject);
        }
    }

    @FXML
    private void reloadKanbanPage(ActionEvent event) {
        kanbanAllTasks.clear();
        if (kanbanProjectId <= 0) {
            renderKanbanColumns(List.of());
            return;
        }
        int uid = AppState.getUserId();
        if (uid <= 0) {
            renderKanbanColumns(List.of());
            return;
        }
        try {
            List<KanbanTask> list = kanbanRepo.listByProjectAndStudent(kanbanProjectId, uid);
            kanbanAllTasks.addAll(list);
            renderKanbanFiltered();
        } catch (Exception e) {
            error("Erreur Kanban", e);
        }
    }

    @FXML
    private void addKanbanTaskOnPage(ActionEvent event) {
        if (kanbanProjectId <= 0) {
            info("Kanban", "Aucun projet sélectionné.");
            return;
        }
        int uid = AppState.getUserId();
        if (uid <= 0) {
            info("Kanban", "Compte non identifié (userId manquant).");
            return;
        }

        String title = text(kanbanTaskTitleField);
        if (title.isBlank()) {
            warn("Kanban", "Titre de tâche requis (2–16 lettres).");
            return;
        }
        String err = ProjectRules.kanbanTitleValidationError(title);
        if (err != null) {
            warn("Kanban — Attention", "Problème: " + err + "\nCorrigez puis réessayez.");
            return;
        }

        KanbanStatus st = kanbanTaskStatusCombo == null ? KanbanStatus.TODO : kanbanTaskStatusCombo.getValue();
        if (st == null) {
            st = KanbanStatus.TODO;
        }

        KanbanTask t = new KanbanTask();
        t.setTitle(title);
        t.setDescription(text(kanbanTaskDescArea));
        t.setStatus(st);
        t.setProjectId(kanbanProjectId);
        t.setStudentId(uid);

        try {
            kanbanRepo.create(t);
            if (kanbanTaskTitleField != null) {
                kanbanTaskTitleField.clear();
            }
            if (kanbanTaskDescArea != null) {
                kanbanTaskDescArea.clear();
            }
            reloadKanbanPage(null);
        } catch (Exception e) {
            error("Erreur", e);
        }
    }

    private void openKanban(int projectId, String projectTitle, KanbanReturn ret) {
        kanbanProjectId = Math.max(0, projectId);
        kanbanProjectTitle = safe(projectTitle);
        kanbanReturn = ret == null ? KanbanReturn.SUBMIT : ret;

        if (kanbanTitleLabel != null) {
            String t = kanbanProjectTitle;
            kanbanTitleLabel.setText(t.isBlank() ? "Tableau Kanban" : ("Tableau Kanban — " + t));
        }
        if (kanbanSearchField != null) {
            kanbanSearchField.clear();
        }

        setView(kanbanPane);
        // hide top search/sort controls when viewing kanban
        setTopControlsVisible(false);
        refreshKanbanValidation();
        reloadKanbanPage(null);
    }

    private void fillProjectLabels(Project project, Label titleLabel, Label metaLabel, Label descLabel) {
        if (project == null) {
            return;
        }
        if (titleLabel != null) {
            titleLabel.setText(safe(project.getTitle()));
        }
        String deadline = safe(project.getDeadline());
        String deliverables = safe(project.getDeliverables());
        String meta = (deadline.isBlank() ? "" : ("Deadline: " + deadline + "  ")) + (deliverables.isBlank() ? "" : ("Livrables: " + deliverables));
        if (metaLabel != null) {
            metaLabel.setText(meta.isBlank() ? ("Projet #" + project.getId()) : meta);
        }
        if (descLabel != null) {
            descLabel.setText(safe(project.getDescription()));
        }
    }

    @FXML
    private void refreshProjectMeetingStatus(ActionEvent event) {
        if (selectedProject == null) {
            info("Meeting", "Selectionnez un projet.");
            return;
        }
        Project refreshed = refreshProjectState(selectedProject);
        if (viewPane != null && viewPane.isVisible()) {
            showView(refreshed);
        } else if (submitPane != null && submitPane.isVisible()) {
            showSubmit(refreshed);
        } else {
            updateMeetingPanel(refreshed);
        }
    }

    @FXML
    private void joinProjectMeeting(ActionEvent event) {
        openProjectMeeting(false);
    }

    @FXML
    private void joinProjectMeetingMuted(ActionEvent event) {
        openProjectMeeting(true);
    }

    @FXML
    private void copyProjectMeetingLink(ActionEvent event) {
        Project project = refreshProjectState(selectedProject);
        if (project == null || !project.isMeetingActive() || safe(project.getMeetingUrl()).isBlank()) {
            info("Meeting", "Le professeur n'a pas encore ouvert de salle pour ce projet.");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(project.getMeetingUrl());
        Clipboard.getSystemClipboard().setContent(content);
        updateMeetingPanel(project);
        info("Meeting", "Lien de meeting copie.");
    }

    private void openProjectMeeting(boolean muted) {
        Project project = refreshProjectState(selectedProject);
        if (project == null) {
            info("Meeting", "Selectionnez un projet.");
            return;
        }
        if (!project.isMeetingActive()) {
            updateMeetingPanel(project);
            info("Meeting", "Le professeur n'a pas encore ouvert de salle pour ce projet.");
            return;
        }
        try {
            String joinUrl = projectMeetingService.joinUrl(project, muted);
            String room = safe(project.getMeetingRoom());
            String title = "EduCompus | Meeting | " + (room.isBlank() ? ("Project " + project.getId()) : room);
            browserService.openMeetingDialog(title, joinUrl);
            updateMeetingPanel(project);
        } catch (Exception e) {
            error("Meeting", e);
        }
    }

    private Project refreshProjectState(Project project) {
        Project refreshed = projectMeetingService.refresh(project);
        if (refreshed != null) {
            selectedProject = refreshed;
            replaceProjectInCollections(refreshed);
            return refreshed;
        }
        return project;
    }

    private void replaceProjectInCollections(Project updated) {
        replaceProjectInList(allProjects, updated);
        replaceProjectInList(projects, updated);
    }

    private static void replaceProjectInList(ObservableList<Project> list, Project updated) {
        if (list == null || updated == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Project current = list.get(i);
            if (current != null && current.getId() == updated.getId()) {
                list.set(i, updated);
                return;
            }
        }
    }

    private void updateMeetingPanel(Project project) {
        String status = projectMeetingService.statusText(project);
        String link = project == null || safe(project.getMeetingUrl()).isBlank()
                ? "Invite link will appear here when the teacher opens the room."
                : project.getMeetingUrl();
        boolean active = project != null && project.isMeetingActive() && !safe(project.getMeetingUrl()).isBlank();

        if (viewMeetingStatusLabel != null) {
            viewMeetingStatusLabel.setText(status);
        }
        if (submitMeetingStatusLabel != null) {
            submitMeetingStatusLabel.setText(status);
        }
        if (viewMeetingLinkLabel != null) {
            viewMeetingLinkLabel.setText(link);
        }
        if (viewMeetingOpenButton != null) {
            viewMeetingOpenButton.setDisable(!active);
        }
        if (viewMeetingMutedButton != null) {
            viewMeetingMutedButton.setDisable(!active);
        }
        if (viewMeetingCopyButton != null) {
            viewMeetingCopyButton.setDisable(!active);
        }
        if (submitMeetingOpenButton != null) {
            submitMeetingOpenButton.setDisable(!active);
        }
        if (submitMeetingMutedButton != null) {
            submitMeetingMutedButton.setDisable(!active);
        }
        if (submitMeetingCopyButton != null) {
            submitMeetingCopyButton.setDisable(!active);
        }
    }

    private void setView(VBox active) {
        setPaneVisible(cataloguePane, active == cataloguePane);
        setPaneVisible(viewPane, active == viewPane);
        setPaneVisible(submitPane, active == submitPane);
        setPaneVisible(kanbanPane, active == kanbanPane);
        setPaneVisible(mySubmissionsPane, active == mySubmissionsPane);
    }

    private void setPaneVisible(VBox pane, boolean on) {
        if (pane == null) {
            return;
        }
        pane.setVisible(on);
        pane.setManaged(on);
    }

    @FXML
    private void chooseCahier(ActionEvent event) {
        File f = chooseFile("Choisir cahier des charges", List.of("pdf", "doc", "docx", "ppt", "pptx", "jpg", "jpeg", "png"), "Documents");
        if (f != null && cahierPathField != null) {
            cahierPathField.setText(f.getAbsolutePath());
        }
    }

    @FXML
    private void chooseDossier(ActionEvent event) {
        File f = chooseFile("Choisir dossier / ZIP", List.of("zip", "rar", "7z"), "Archives");
        if (f != null && dossierPathField != null) {
            dossierPathField.setText(f.getAbsolutePath());
        }
    }

    private File chooseFile(String title, List<String> allowedExt, String filterLabel) {
        Window w = projectSearchField == null ? null : projectSearchField.getScene() == null ? null : projectSearchField.getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setTitle(title == null ? "Choisir un fichier" : title);
        // if no explicit allowed extensions provided, allow common document/image/archive types
        if (allowedExt == null || allowedExt.isEmpty()) {
            allowedExt = List.of("pdf","doc","docx","ppt","pptx","xls","xlsx","csv","jpg","jpeg","png","gif","bmp","zip","rar","7z");
        }
        if (allowedExt != null && !allowedExt.isEmpty()) {
            List<String> patterns = new ArrayList<>();
            for (String ext : allowedExt) {
                String e = safe(ext).toLowerCase();
                if (!e.isBlank()) {
                    patterns.add("*." + e);
                }
            }
            if (!patterns.isEmpty()) {
                FileChooser.ExtensionFilter all = new FileChooser.ExtensionFilter("All Files", "*.*");
                FileChooser.ExtensionFilter specific = new FileChooser.ExtensionFilter(safe(filterLabel).isBlank() ? "Fichiers" : filterLabel, patterns);
                fc.getExtensionFilters().setAll(all, specific);
                // prefer to show 'All Files' by default so user can pick any file
                fc.setSelectedExtensionFilter(all);
            } else {
                fc.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
            }
        }
        try {
            return fc.showOpenDialog(w);
        } catch (Exception e) {
            error("Fichier", e);
            return null;
        }
    }

    @FXML
    private void submit(ActionEvent event) {
        if (selectedProject == null) {
            info("Soumission", "Sélectionnez un projet.");
            return;
        }
        int uid = AppState.getUserId();
        if (uid <= 0) {
            info("Soumission", "Compte non identifié (userId manquant). Reconnectez-vous.");
            return;
        }

        String err = ProjectRules.validateSubmissionFields(text(responseArea), text(cahierPathField), text(dossierPathField));
        if (err != null) {
            info("Soumission", err);
            refreshSubmitValidation();
            return;
        }

        if (editingSubmissionId > 0) {
            ProjectSubmission s = new ProjectSubmission();
            s.setId(editingSubmissionId);
            s.setProjectId(selectedProject.getId());
            s.setStudentId(uid);
            s.setTextResponse(text(responseArea));
            s.setCahierPath(text(cahierPathField));
            s.setDossierPath(text(dossierPathField));

            try {
                submissionRepo.updateMine(s);
                info("Soumission", "Soumission mise a jour.");
                stopEditingAndResetForm();
                reloadSubmissions(null);
                selectSubmissionById(s.getId());
                showMySubmissions();
            } catch (Exception e) {
                error("Erreur", e);
            }
            return;
        }

        if (submissionRepo.hasSubmissionMine(selectedProject.getId(), uid, AppState.getUserEmail())) {
            info("Soumission", "Vous avez déjà soumis ce projet.");
            refreshSubmitInfo();
            return;
        }

        ProjectSubmission s = new ProjectSubmission();
        s.setProjectId(selectedProject.getId());
        s.setStudentId(uid);
        s.setTextResponse(text(responseArea));
        s.setCahierPath(text(cahierPathField));
        s.setDossierPath(text(dossierPathField));

        try {
            submissionRepo.create(s);
            notificationRepo.createProjectSubmissionNotifications(
                    selectedProject.getId(),
                    uid,
                    AppState.getUserEmail(),
                    selectedProject.getTitle()
            );
            resetSubmitFormFields();
            info("Soumission", "Votre soumission a été enregistrée.");
            refreshSubmitInfo();
            reloadSubmissions(null);
        } catch (Exception e) {
            error("Erreur", e);
        }
    }

    private void refreshSubmitInfo() {
        if (submitInfoLabel == null || selectedProject == null) {
            return;
        }
        if (editingSubmissionId > 0) {
            submitInfoLabel.setText("Mode modification.");
            return;
        }
        int uid = AppState.getUserId();
        String mail = AppState.getUserEmail();
        if (uid <= 0) {
            if (mail == null || mail.isBlank()) {
                submitInfoLabel.setText("Compte non identifié.");
                return;
            }
        }
        boolean already = submissionRepo.hasSubmissionMine(selectedProject.getId(), uid, mail);
        submitInfoLabel.setText(already ? "Soumission déjà envoyée." : "");
    }

    @FXML
    private void reloadSubmissions(ActionEvent event) {
        int keepId = selectedSubmission == null ? 0 : selectedSubmission.getId();
        int uid = AppState.getUserId();
        String mail = AppState.getUserEmail();
        List<ProjectSubmissionView> mine = submissionRepo.listMine(uid, mail);
        // keep full list, then apply filter+sort
        allSubmissions.setAll(mine);
        applySubmissionsFilterAndSort();

        if (keepId > 0) {
            selectSubmissionById(keepId);
        } else if (!submissions.isEmpty() && submissionsList != null && submissionsList.getSelectionModel().getSelectedItem() == null) {
            submissionsList.getSelectionModel().selectFirst();
        }
    }

    private void applySubmissionsFilterAndSort() {
        List<ProjectSubmissionView> list = new ArrayList<>();
        String q = submissionsSearchField == null ? "" : safe(submissionsSearchField.getText()).toLowerCase();
        for (ProjectSubmissionView s : allSubmissions) {
            if (s == null) continue;
            boolean keep = true;
            if (submissionsProjectFilterId > 0 && s.getProjectId() != submissionsProjectFilterId) {
                keep = false;
            }
            if (keep && !q.isBlank()) {
                String t = safe(s.getProjectTitle()).toLowerCase();
                String r = safe(s.getTextResponse()).toLowerCase();
                String date = safe(s.getSubmittedAt()).toLowerCase();
                if (!(t.contains(q) || r.contains(q) || date.contains(q))) {
                    keep = false;
                }
            }
            if (keep) list.add(s);
        }

        String sort = submissionsSortCombo == null ? "" : safe(submissionsSortCombo.getValue());
        if ("Projet A-Z".equalsIgnoreCase(sort)) {
            list.sort(Comparator.comparing((ProjectSubmissionView s) -> safe(s == null ? null : s.getProjectTitle()).toLowerCase()));
        } else if ("Date asc".equalsIgnoreCase(sort)) {
            list.sort(Comparator.comparing((ProjectSubmissionView s) -> safe(s == null ? null : s.getSubmittedAt())));
        } else {
            list.sort(Comparator.comparing((ProjectSubmissionView s) -> safe(s == null ? null : s.getSubmittedAt())).reversed());
        }

        submissions.setAll(list);
    }

    private void openMySubmissionsForProject(Project project) {
        if (project == null) {
            clearSubmissionsProjectFilter();
            showMySubmissions();
            reloadSubmissions(null);
            return;
        }
        submissionsProjectFilterId = project.getId();
        submissionsProjectFilterTitle = safe(project.getTitle());
        updateMySubmissionsTitle();
        showMySubmissions();
        reloadSubmissions(null);
    }

    private void clearSubmissionsProjectFilter() {
        submissionsProjectFilterId = 0;
        submissionsProjectFilterTitle = "";
        updateMySubmissionsTitle();
    }

    private void updateMySubmissionsTitle() {
        if (mySubmissionsTitleLabel == null) {
            return;
        }
        if (submissionsProjectFilterId > 0) {
            String t = submissionsProjectFilterTitle;
            mySubmissionsTitleLabel.setText(t.isBlank() ? "Mes soumissions (projet)" : ("Mes soumissions — " + t));
        } else {
            mySubmissionsTitleLabel.setText("Mes soumissions");
        }
    }

    private void updateSelectedSubmissionLabel() {
        if (selectedSubmissionLabel == null) {
            return;
        }
        if (selectedSubmission == null) {
            selectedSubmissionLabel.setText("Sélectionnez une soumission");
            return;
        }
        String p = safe(selectedSubmission.getProjectTitle());
        selectedSubmissionLabel.setText("Kanban — " + (p.isBlank() ? ("Projet #" + selectedSubmission.getProjectId()) : p));
    }

    private void startEditingSubmission(ProjectSubmissionView submission) {
        editingSubmissionId = submission == null ? 0 : submission.getId();
        if (responseArea != null) {
            responseArea.setText(safe(submission == null ? null : submission.getTextResponse()));
        }
        if (cahierPathField != null) {
            cahierPathField.setText(safe(submission == null ? null : submission.getCahierPath()));
        }
        if (dossierPathField != null) {
            dossierPathField.setText(safe(submission == null ? null : submission.getDossierPath()));
        }
	        updateSubmitButtonLabel();
	        refreshSubmitInfo();
	        refreshSubmitValidation();
	    }

    private void stopEditingAndResetForm() {
        editingSubmissionId = 0;
	        resetSubmitFormFields();
	        updateSubmitButtonLabel();
	        refreshSubmitInfo();
	        refreshSubmitValidation();
	    }

    private void updateSubmitButtonLabel() {
        if (submitButton == null) {
            return;
        }
        submitButton.setText(editingSubmissionId > 0 ? "Mettre a jour" : "Envoyer");
    }

    private void resetSubmitFormFields() {
        if (responseArea != null) {
            responseArea.clear();
        }
        if (cahierPathField != null) {
            cahierPathField.clear();
        }
        if (dossierPathField != null) {
            dossierPathField.clear();
        }
    }

    private void selectSubmissionById(int id) {
        if (id <= 0 || submissionsList == null) {
            return;
        }
        for (ProjectSubmissionView s : submissions) {
            if (s != null && s.getId() == id) {
                submissionsList.getSelectionModel().select(s);
                return;
            }
        }
        if (!submissions.isEmpty()) {
            submissionsList.getSelectionModel().selectFirst();
        }
    }

    private Project findProjectById(int id) {
        if (id <= 0) {
            return null;
        }
        for (Project p : allProjects) {
            if (p != null && p.getId() == id) {
                return p;
            }
        }
        for (Project p : projects) {
            if (p != null && p.getId() == id) {
                return p;
            }
        }
        return null;
    }

    private void downloadFromPath(String rawPath, String kind) {
        String p = safe(rawPath);
        if (p.isBlank()) {
            return;
        }
        try {
            if (p.startsWith("http://") || p.startsWith("https://")) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI.create(p));
                } else {
                    info("Télécharger", "Lien: " + p);
                }
                return;
            }

            File src = new File(p);
            if (!src.exists() || !src.isFile()) {
                info("Télécharger", "Fichier introuvable: " + p);
                return;
            }

            FileChooser fc = new FileChooser();
            fc.setTitle("Télécharger " + (kind == null ? "fichier" : kind));
            fc.setInitialFileName(src.getName());
            String ext = ProjectRules.extensionOf(src.getName());
            if (!ext.isBlank()) {
                fc.getExtensionFilters().setAll(new FileChooser.ExtensionFilter(ext.toUpperCase() + " (*." + ext + ")", "*." + ext));
                fc.setSelectedExtensionFilter(fc.getExtensionFilters().get(0));
            }
            Window w = projectSearchField == null ? null : projectSearchField.getScene() == null ? null : projectSearchField.getScene().getWindow();
            File dest = fc.showSaveDialog(w);
            if (dest == null) {
                return;
            }
            if (!ext.isBlank() && ProjectRules.extensionOf(dest.getName()).isBlank()) {
                dest = new File(dest.getParentFile(), dest.getName() + "." + ext);
            }
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            info("Télécharger", "Enregistre: " + dest.getAbsolutePath());
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(dest);
                } catch (Exception ignored) {
                    // ignore
                }
            }
        } catch (Exception e) {
            error("Télécharger", e);
        }
    }

    private static String extensionOf(String filename) {
        String v = safe(filename);
        int idx = v.lastIndexOf('.');
        if (idx <= 0 || idx >= v.length() - 1) {
            return "";
        }
        String ext = v.substring(idx + 1).trim().toLowerCase();
        if (ext.length() > 12) {
            return "";
        }
        if (!ext.matches("[a-z0-9]+")) {
            return "";
        }
        return ext;
    }

    private void renderKanbanFiltered() {
        String q = kanbanSearchField == null ? "" : safe(kanbanSearchField.getText()).toLowerCase();
        if (q.isBlank()) {
            renderKanbanColumns(kanbanAllTasks);
            return;
        }
        List<KanbanTask> filtered = new ArrayList<>();
        for (KanbanTask t : kanbanAllTasks) {
            if (t == null) {
                continue;
            }
            String title = safe(t.getTitle()).toLowerCase();
            String desc = safe(t.getDescription()).toLowerCase();
            if (title.contains(q) || desc.contains(q)) {
                filtered.add(t);
            }
        }
        renderKanbanColumns(filtered);
    }

    private void renderKanbanColumns(List<KanbanTask> tasks) {
        Map<KanbanStatus, List<KanbanTask>> grouped = new EnumMap<>(KanbanStatus.class);
        for (KanbanStatus s : KanbanStatus.values()) {
            grouped.put(s, new ArrayList<>());
        }
        for (KanbanTask t : tasks) {
            grouped.getOrDefault(t.getStatus(), grouped.get(KanbanStatus.TODO)).add(t);
        }
        for (var entry : grouped.entrySet()) {
            entry.getValue().sort(Comparator.comparingInt(KanbanTask::getPosition).thenComparingInt(KanbanTask::getId));
        }

        renderColumn(kanbanTodoColumn, KanbanStatus.TODO, grouped.get(KanbanStatus.TODO));
        renderColumn(kanbanInProgressColumn, KanbanStatus.IN_PROGRESS, grouped.get(KanbanStatus.IN_PROGRESS));
        renderColumn(kanbanDoneColumn, KanbanStatus.DONE, grouped.get(KanbanStatus.DONE));
    }

    private void renderColumn(VBox column, KanbanStatus status, List<KanbanTask> tasks) {
        if (column == null) {
            return;
        }
        column.getChildren().clear();

        Label header = new Label(status.label());
        header.getStyleClass().add("kanban-column-title");
        column.getChildren().add(header);

	        for (KanbanTask t : tasks) {
	            VBox card = new VBox(6);
	            card.getStyleClass().add("kanban-card");
	            card.setOnDragDetected(e -> {
	                Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
	                ClipboardContent cc = new ClipboardContent();
	                cc.putString(String.valueOf(t.getId()));
	                db.setContent(cc);
	                e.consume();
	            });

            Label title = new Label(safe(t.getTitle()));
            title.getStyleClass().add("kanban-card-title");

            String desc = safe(t.getDescription());
            if (!desc.isBlank()) {
                Tooltip.install(title, new Tooltip(desc));
            }

            Region grow = new Region();
            HBox.setHgrow(grow, Priority.ALWAYS);

	            Button edit = new Button("Modifier");
	            edit.getStyleClass().add("btn-rgb-outline");
	            edit.setOnAction(e -> editTask(t));

            Button del = new Button("✕");
            del.getStyleClass().add("btn-danger");
            del.setOnAction(e -> deleteTask(t));

	            HBox actions = new HBox(8, edit, grow, del);
	            actions.getStyleClass().add("kanban-card-actions");

            card.getChildren().addAll(title, actions);
            column.getChildren().add(card);
        }
    }

    private void moveTask(KanbanTask task, KanbanStatus target) {
        if (task == null || target == null) {
            return;
        }
        try {
            kanbanRepo.updateStatus(task.getId(), target);
            reloadKanbanPage(null);
        } catch (Exception e) {
            error("Erreur", e);
        }
    }

    private void deleteTask(KanbanTask task) {
        if (task == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer tâche");
        confirm.setHeaderText("Supprimer la tâche ?");
        confirm.setContentText("Tâche: " + safe(task.getTitle()));
        styleDialog(confirm);
        var res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }

        try {
            kanbanRepo.delete(task.getId());
            reloadKanbanPage(null);
        } catch (Exception e) {
            error("Erreur", e);
        }
    }

    private void editTask(KanbanTask task) {
        if (task == null) {
            return;
        }
        int uid = AppState.getUserId();
        if (uid <= 0) {
            info("Kanban", "Compte non identifie.");
            return;
        }

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Modifier tache");
        d.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);

        TextField titleField = new TextField(safe(task.getTitle()));
        titleField.getStyleClass().add("field");
        titleField.setPromptText("Titre");
        // enforce typing constraints while editing: no digits, only letters/spaces/apostrophe/hyphen, max 16 chars
        javafx.scene.control.TextFormatter<String> editTf = new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.length() > 16) {
                return null;
            }
            if (!newText.matches("[\\p{L}\\s'\\-]*")) {
                return null;
            }
            return change;
        });
        titleField.setTextFormatter(editTf);
        // hide description in edit dialog (not editable here)
        TextArea descArea = new TextArea(safe(task.getDescription()));
        descArea.setVisible(false);
        descArea.setManaged(false);

        ComboBox<KanbanStatus> statusCombo = new ComboBox<>();
        initStatusCombo(statusCombo);
        statusCombo.getStyleClass().add("combo-rgb");
        statusCombo.setValue(task.getStatus() == null ? KanbanStatus.TODO : task.getStatus());

        VBox box = new VBox(10,
            new Label("Titre"), titleField,
            new Label("Statut"), statusCombo
        );
        box.setPadding(new Insets(10));
        d.getDialogPane().setContent(box);

        styleDialog(d);
        var res = d.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }

        // validate after user accepted; show specific warning if invalid
            String err = ProjectRules.kanbanTitleValidationError(titleField.getText());
        if (err != null) {
            warn("Kanban — Attention", "Problème: " + err + "\nCorrigez puis réessayez.");
            return;
        }

        KanbanTask updated = new KanbanTask();
        updated.setId(task.getId());
        updated.setProjectId(task.getProjectId());
        updated.setStudentId(uid);
        updated.setTitle(text(titleField));
        updated.setDescription("");
        updated.setStatus(statusCombo.getValue() == null ? KanbanStatus.TODO : statusCombo.getValue());

        try {
            kanbanRepo.updateMine(updated);
            reloadKanbanPage(null);
        } catch (Exception e) {
            error("Erreur Kanban", e);
        }
    }

    private static KanbanStatus previous(KanbanStatus s) {
        if (s == KanbanStatus.IN_PROGRESS) {
            return KanbanStatus.TODO;
        }
        if (s == KanbanStatus.DONE) {
            return KanbanStatus.IN_PROGRESS;
        }
        return KanbanStatus.TODO;
    }

    private static KanbanStatus next(KanbanStatus s) {
        if (s == KanbanStatus.TODO) {
            return KanbanStatus.IN_PROGRESS;
        }
        if (s == KanbanStatus.IN_PROGRESS) {
            return KanbanStatus.DONE;
        }
        return KanbanStatus.DONE;
    }

    private static String text(TextField tf) {
        return tf == null ? "" : String.valueOf(tf.getText()).trim();
    }

    private static String text(TextArea ta) {
        return ta == null ? "" : String.valueOf(ta.getText()).trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String summarize(String s, int max) {
        String v = safe(s).replace('\n', ' ').replace('\r', ' ').trim();
        if (v.length() <= max) {
            return v;
        }
        return v.substring(0, Math.max(0, max - 1)).trim() + "…";
    }

    private static String deadlineChipText(String deadline) {
        String d = safe(deadline);
        return d.isBlank() ? "Sans date" : d;
    }

    private static void info(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        styleDialog(a);
        a.showAndWait();
    }

    private static void warn(String title, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        // red exclamation icon
        Label icon = new Label("!");
        icon.setStyle("-fx-text-fill: white; -fx-background-color: #e53935; -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-background-radius: 20; -fx-font-size: 14px;");
        a.getDialogPane().setGraphic(icon);
        styleDialog(a);
        a.showAndWait();
    }

    private static void error(String title, Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(e == null ? "Erreur" : String.valueOf(e.getMessage()));
        styleDialog(a);
        a.showAndWait();
        if (e != null) {
            e.printStackTrace();
        }
    }

    private static boolean confirmRgb(String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        // red X icon for delete confirmation
        Label icon = new Label("✕");
        icon.setStyle("-fx-text-fill: white; -fx-background-color: #e53935; -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-background-radius: 20; -fx-font-size: 14px;");
        a.getDialogPane().setGraphic(icon);
        styleDialog(a);
        var res = a.showAndWait();
        return res.isPresent() && res.get() == ButtonType.OK;
    }

    private static void styleDialog(Dialog<?> d) {
        if (d == null || d.getDialogPane() == null) {
            return;
        }
        String css = cssUri();
        if (!css.isBlank() && !d.getDialogPane().getStylesheets().contains(css)) {
            d.getDialogPane().getStylesheets().add(css);
        }
        if (!d.getDialogPane().getStyleClass().contains("rgb-dialog")) {
            d.getDialogPane().getStyleClass().add("rgb-dialog");
        }

        for (ButtonType bt : d.getDialogPane().getButtonTypes()) {
            Node b = d.getDialogPane().lookupButton(bt);
            if (b == null) continue;
            if (bt == ButtonType.OK) {
                b.getStyleClass().add("btn-rgb");
            } else if (bt == ButtonType.CANCEL) {
                b.getStyleClass().add("btn-rgb-outline");
            }
        }
    }

    private static String cssUri() {
        if (cssUriCache != null) {
            return cssUriCache;
        }
        File f = new File("styles/educompus.css");
        if (!f.exists()) {
            f = new File("eduCompus-javafx/styles/educompus.css");
        }
        if (!f.exists()) {
            f = new File(new File("..", "eduCompus-javafx"), "styles/educompus.css");
        }
        cssUriCache = f.exists() ? f.toURI().toString() : "";
        return cssUriCache;
    }

    private static String cssUriCache = null;
}

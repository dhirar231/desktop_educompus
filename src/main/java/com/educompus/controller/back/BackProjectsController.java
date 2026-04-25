package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.model.Project;
import com.educompus.model.ProjectSubmissionView;
import com.educompus.repository.ProjectRepository;
import com.educompus.repository.ProjectSubmissionRepository;
import com.educompus.service.FormValidator;
import com.educompus.service.JcefBrowserService;
import com.educompus.service.ProjectMeetingService;
import com.educompus.service.ProjectValidationService;
import com.educompus.service.ValidationResult;
import com.educompus.util.Theme;
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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.util.Callback;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BackProjectsController {
    private final ProjectRepository projectRepo = new ProjectRepository();
    private final ProjectSubmissionRepository submissionRepo = new ProjectSubmissionRepository();
    private final ProjectMeetingService projectMeetingService = new ProjectMeetingService();
    private final JcefBrowserService browserService = JcefBrowserService.getInstance();

    @FXML
    private MenuButton projectsMenuButton;

    @FXML
    private MenuItem catalogueMenuItem;

    @FXML
    private Button sendMailButton;

    @FXML
    private StackPane viewStack;

    @FXML
    private VBox projectsPane;

    @FXML
    private VBox submissionsPane;

    @FXML
    private VBox cataloguePane;

    @FXML
    private TextField projectSearchField;

    @FXML
    private ComboBox<String> projectsSortCombo;

    @FXML
    private javafx.scene.control.ListView<Project> projectsList;
    @FXML
    private Button projectsPrevBtn;
    @FXML
    private Button projectsNextBtn;
    @FXML
    private Label projectsPageLabel;

    @FXML
    private TextField titleField;

    @FXML
    private javafx.scene.control.DatePicker deadlineDatePicker;

    @FXML
    private TextField deadlineTimeField;

    @FXML
    private TextArea deliverablesArea;

    @FXML
    private TextArea descriptionArea;
    @FXML
    private Label descriptionError;
    @FXML
    private Label deadlineError;

    @FXML
    private CheckBox publishedCheck;

    @FXML
    private javafx.scene.control.ListView<ProjectSubmissionView> submissionsList;

    @FXML
    private TextField submissionsSearchField;

    @FXML
    private ComboBox<String> submissionsSortCombo;

    @FXML
    private FlowPane adminCardsFlow;

    private final ObservableList<Project> projects = FXCollections.observableArrayList();
    private final List<Project> allProjects = new ArrayList<>();
    private int projectsPageSize = 10;
    private int projectsCurrentPage = 1;
    private final ObservableList<ProjectSubmissionView> submissions = FXCollections.observableArrayList();
    private final ObservableList<ProjectSubmissionView> allSubmissions = FXCollections.observableArrayList();
    private final Map<Integer, Integer> submissionsCountByProject = new HashMap<>();

    private Project editingProject;
    private ProjectSubmissionView selectedSubmission;

    @FXML
    private void initialize() {
        boolean adminOnlyCatalogue = AppState.isAdmin() || AppState.isTeacher();
        if (catalogueMenuItem != null) {
            catalogueMenuItem.setVisible(adminOnlyCatalogue);
            catalogueMenuItem.setDisable(!adminOnlyCatalogue);
        }
        if (sendMailButton != null) {
            boolean canMail = AppState.isAdmin() || AppState.isTeacher();
            sendMailButton.setVisible(canMail);
            sendMailButton.setManaged(canMail);
        }
        if (cataloguePane != null && !adminOnlyCatalogue) {
            cataloguePane.setVisible(false);
            cataloguePane.setManaged(false);
        }

        setupProjectsTable();
        setupSubmissionsTable();
        if (adminOnlyCatalogue) {
            setupAdminCatalogue();
        }
        setupProjectsSort();

        reloadProjects(null);
        reloadSubmissions(null);
        showProjectsView(null);
    }

    private void setupProjectsSort() {
        if (projectSearchField != null) {
            projectSearchField.textProperty().addListener((obs, o, n) -> reloadProjects(null));
        }
        if (projectsSortCombo != null) {
            projectsSortCombo.getItems().setAll("Date desc", "Date asc", "Projet A-Z", "Projet Z-A", "Deadline");
            if (projectsSortCombo.getValue() == null || String.valueOf(projectsSortCombo.getValue()).isBlank()) {
                projectsSortCombo.setValue("Date desc");
            }
            projectsSortCombo.valueProperty().addListener((obs, o, n) -> applyProjectsSort());
        }
    }

    @FXML
    private void showProjectsView(ActionEvent event) {
        setView(projectsPane);
        if (projectsMenuButton != null) {
            projectsMenuButton.setText("Projets");
        }
        reloadProjects(null);
    }

    @FXML
    private void showSubmissionsView(ActionEvent event) {
        setView(submissionsPane);
        if (projectsMenuButton != null) {
            projectsMenuButton.setText("Soumissions");
        }
        reloadSubmissions(null);
    }

    @FXML
    private void showCatalogueView(ActionEvent event) {
        if (!(AppState.isAdmin() || AppState.isTeacher())) {
            showProjectsView(null);
            return;
        }
        setView(cataloguePane);
        if (projectsMenuButton != null) {
            projectsMenuButton.setText("Catalogue (cartes)");
        }
        reloadProjects(null);
    }

    @FXML
    private void openKanbanForSelectedSubmission(ActionEvent event) {
        if (selectedSubmission == null) {
            info("Kanban", "Sélectionnez une soumission d'abord.");
            return;
        }
        openKanbanWindow(selectedSubmission);
    }

    @FXML
    private void openMailingDialog(ActionEvent event) {
        if (!(AppState.isAdmin() || AppState.isTeacher())) {
            info("Mailing", "Acces reserve a l'administration et aux enseignants.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/View/back/BackProjectMailing.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 760, 760);
            if (sendMailButton != null && sendMailButton.getScene() != null) {
                scene.getStylesheets().addAll(sendMailButton.getScene().getStylesheets());
            }
            Theme.apply(root);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Envoyer mail");
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            error("Erreur ouverture mailing", e);
        }
    }

    private void setView(VBox pane) {
        setPaneVisible(projectsPane, pane == projectsPane);
        setPaneVisible(submissionsPane, pane == submissionsPane);
        setPaneVisible(cataloguePane, pane == cataloguePane);
    }

    private static void setPaneVisible(VBox pane, boolean on) {
        if (pane == null) {
            return;
        }
        pane.setVisible(on);
        pane.setManaged(on);
    }

    private void setupAdminCatalogue() {
        if (adminCardsFlow == null) return;
        // bind wrap length so we aim for 3 cards per row when space allows
        try {
            adminCardsFlow.prefWrapLengthProperty().bind(projectsPane.widthProperty().subtract(60));
        } catch (Exception ignore) {}
        // render cards initially
        renderAdminCards();
    }

    private void renderAdminCards() {
        if (!(AppState.isAdmin() || AppState.isTeacher()) || adminCardsFlow == null) return;
        adminCardsFlow.getChildren().clear();
        // Aim for 3 cards per row: compute card width based on available width
        double avail = 900;
        try {
            if (projectsPane != null && projectsPane.getWidth() > 0) {
                avail = Math.max(760, projectsPane.getWidth() - 80);
            }
        } catch (Exception ignore) {}
        int cols = 2; // admin: force 2 cards per row
        double hgap = adminCardsFlow.getHgap();
        double cardW = (avail - (cols - 1) * hgap) / cols;
        // clamp card width to reasonable range for 2-column layout
        cardW = Math.max(320, Math.min(520, cardW));

        for (Project p : allProjects) {
            if (p == null) continue;
            VBox card = buildAdminProjectCard(p);
            card.setPrefWidth(cardW);
            card.setMinWidth(180);
            adminCardsFlow.getChildren().add(card);
        }
    }

    private VBox buildAdminProjectCard(Project project) {
        final Project currentProject = refreshProjectState(project);
        VBox card = new VBox(10);
        card.getStyleClass().add("project-card");
        card.setPadding(new javafx.geometry.Insets(14));
        card.setPrefWidth(260);
        card.setMinWidth(240);

        HBox top = new HBox(10);
        Label title = new Label(safe(currentProject.getTitle()));
        title.getStyleClass().add("project-card-title");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label chip = new Label(safe(currentProject.getDeadline()));
        chip.getStyleClass().addAll("chip", "chip-info");
        // keep the deadline chip compact so it doesn't stretch across the card
        chip.setWrapText(false);
        chip.setPrefWidth(140);
        chip.setMaxWidth(220);
        chip.setMinWidth(Region.USE_PREF_SIZE);

        // populate the top row with title + deadline chip
        top.getChildren().addAll(title, chip);

        // published badge: show emoji in top-right (✅ / ❌)
        boolean published = currentProject.isPublished();
        Label pubBadge = new Label(published ? "✅" : "❌");
        pubBadge.getStyleClass().addAll("pub-emoji");
        pubBadge.setStyle("-fx-font-size:18px; -fx-background-color: transparent; -fx-padding: 4 6 4 6;");
        javafx.scene.control.Tooltip.install(pubBadge, new javafx.scene.control.Tooltip(published ? "Publié" : "Non publié"));

        // build card content and subtitle (description)
        String desc = safe(currentProject.getDescription());
        Label subtitle = new Label(desc.isBlank() ? "Aucune description" : summarize(desc, 110));
        subtitle.getStyleClass().add("project-card-subtitle");
        subtitle.setWrapText(true);

        Label meetingStatus = new Label(currentProject.isMeetingActive()
                ? ("Meeting actif: " + safe(currentProject.getMeetingRoom()))
                : "Meeting inactif");
        meetingStatus.getStyleClass().add("page-subtitle");
        meetingStatus.setWrapText(true);

        VBox content = new VBox(8);
        content.getChildren().addAll(top, subtitle, meetingStatus);

        // container stack so we can place the badge in the top-right corner
        StackPane container = new StackPane();
        container.getChildren().add(content);
        StackPane.setAlignment(pubBadge, javafx.geometry.Pos.TOP_RIGHT);
        StackPane.setMargin(pubBadge, new javafx.geometry.Insets(8, 8, 0, 0));
        container.getChildren().add(pubBadge);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);

        // button to view submissions for this project
        Button btnViewSubs = new Button("Voir soumissions");
        btnViewSubs.getStyleClass().add("btn-rgb-outline");
        btnViewSubs.setOnAction(e -> {
            try {
                if (submissionsSearchField != null) {
                    submissionsSearchField.setText(safe(currentProject.getTitle()));
                }
                // ensure submissions pane is visible and refreshed
                setView(submissionsPane);
                if (projectsMenuButton != null) projectsMenuButton.setText("Soumissions");
                reloadSubmissions(null);
            } catch (Exception ex) {
                error("Erreur", ex);
            }
        });

        MenuButton meetingMenu = null;
        if (AppState.isTeacher()) {
            meetingMenu = new MenuButton("Meeting");
            meetingMenu.getStyleClass().addAll("meeting-menu-btn",
                    currentProject.isMeetingActive() ? "meeting-menu-btn-active" : "meeting-menu-btn-idle");

            MenuItem openMeetingItem = new MenuItem(currentProject.isMeetingActive() ? "Open meeting" : "Create meeting");
            openMeetingItem.setOnAction(e -> openProjectMeeting(currentProject));

            MenuItem copyMeetingItem = new MenuItem("Copy link");
            copyMeetingItem.setDisable(!currentProject.isMeetingActive() || safe(currentProject.getMeetingUrl()).isBlank());
            copyMeetingItem.setOnAction(e -> copyProjectMeetingLink(currentProject));

            MenuItem closeMeetingItem = new MenuItem("Close meeting");
            closeMeetingItem.setDisable(!currentProject.isMeetingActive());
            closeMeetingItem.setOnAction(e -> closeProjectMeeting(currentProject));

            meetingMenu.getItems().setAll(openMeetingItem, copyMeetingItem, closeMeetingItem);
        }

        // publish/unpublish button (avoid aggressive red style; use RGB outline when published)
        Button btnPublish = new Button(published ? "Dépublier" : "Publier");
        btnPublish.getStyleClass().add(published ? "btn-rgb-outline" : "btn-rgb");
        btnPublish.setOnAction(e -> {
            // confirmation before toggling
            String action = currentProject.isPublished() ? "dépublier" : "publier";
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText((currentProject.isPublished() ? "Dépublier" : "Publier") + " le projet ?");
            confirm.setContentText("Projet: " + safe(currentProject.getTitle()) + "\nVoulez-vous vraiment " + action + " ce projet ?");
            var res = confirm.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) {
                return;
            }
            try {
                currentProject.setPublished(!currentProject.isPublished());
                projectRepo.update(currentProject);
                info("Publication", currentProject.isPublished() ? "Projet publié." : "Projet dépublié.");
                // refresh UI
                if (projectsList != null) projectsList.refresh();
                renderAdminCards();
            } catch (Exception ex) {
                error("Erreur", ex);
            }
        });

        HBox actions = new HBox(10);
        actions.getChildren().addAll(grow, btnViewSubs);
        if (meetingMenu != null) {
            actions.getChildren().add(meetingMenu);
        }
        if (AppState.isAdmin()) {
            actions.getChildren().add(btnPublish);
        }
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        card.getChildren().addAll(container, actions);
        return card;
    }

    private void setupProjectsTable() {
        if (projectsList == null) return;
        projectsList.setItems(projects);
        projectsList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Project p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label title = new Label(safe(p.getTitle()));
                title.getStyleClass().add("stat-value");
                Label meta = new Label("Soumissions: " + submissionsCountByProject.getOrDefault(p.getId(), 0) + "  •  " + deadlineChipText(p.getDeadline()) + "  •  " + (p.isPublished() ? "Publié" : "Non"));
                meta.getStyleClass().add("page-subtitle");
                VBox v = new VBox(4, title, meta);
                setGraphic(v);
            }
        });
        projectsList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            editingProject = newV;
        });
        if (projectSearchField != null) projectSearchField.setOnAction(e -> reloadProjects(null));
    }

    private Project refreshProjectState(Project project) {
        Project refreshed = projectMeetingService.refresh(project);
        if (refreshed == null) {
            return project;
        }
        replaceProjectInList(allProjects, refreshed);
        replaceProjectInList(projects, refreshed);
        return refreshed;
    }

    private static void replaceProjectInList(List<Project> list, Project updated) {
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

    private void openProjectMeeting(Project project) {
        try {
            Project updated = project.isMeetingActive() ? refreshProjectState(project) : projectMeetingService.openMeetingForProject(project);
            String room = safe(updated.getMeetingRoom());
            browserService.openMeetingDialog("EduCompus | Meeting | " + room, projectMeetingService.joinUrl(updated, false));
            replaceProjectInList(allProjects, updated);
            replaceProjectInList(projects, updated);
            renderAdminCards();
            if (projectsList != null) {
                projectsList.refresh();
            }
        } catch (Exception e) {
            error("Meeting", e);
        }
    }

    private void copyProjectMeetingLink(Project project) {
        Project updated = refreshProjectState(project);
        if (updated == null || safe(updated.getMeetingUrl()).isBlank()) {
            info("Meeting", "Aucun lien actif pour ce projet.");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(updated.getMeetingUrl());
        Clipboard.getSystemClipboard().setContent(content);
        info("Meeting", "Lien de meeting copie.");
    }

    private void closeProjectMeeting(Project project) {
        try {
            Project updated = projectMeetingService.closeMeetingForProject(project);
            replaceProjectInList(allProjects, updated);
            replaceProjectInList(projects, updated);
            renderAdminCards();
            if (projectsList != null) {
                projectsList.refresh();
            }
            info("Meeting", "Le meeting du projet a ete ferme.");
        } catch (Exception e) {
            error("Meeting", e);
        }
    }

    @FXML
    private void onProjectsPrev(ActionEvent ev) {
        if (projectsCurrentPage > 1) {
            showProjectsPage(projectsCurrentPage - 1);
        }
    }

    @FXML
    private void onProjectsNext(ActionEvent ev) {
        int total = (int) Math.max(1, Math.ceil((double) allProjects.size() / projectsPageSize));
        if (projectsCurrentPage < total) {
            showProjectsPage(projectsCurrentPage + 1);
        }
    }

    private void showProjectsPage(int page) {
        projectsCurrentPage = Math.max(1, page);
        int total = (int) Math.max(1, Math.ceil((double) allProjects.size() / projectsPageSize));
        int from = (projectsCurrentPage - 1) * projectsPageSize;
        int to = Math.min(allProjects.size(), from + projectsPageSize);
        List<Project> sub = new ArrayList<>();
        if (from < to) sub.addAll(allProjects.subList(from, to));
        projects.setAll(sub);
        if (projectsList != null && !projects.isEmpty() && projectsList.getSelectionModel().getSelectedItem() == null) {
            projectsList.getSelectionModel().selectFirst();
        }
        if (projectsPageLabel != null) projectsPageLabel.setText("Page " + projectsCurrentPage + " / " + total);
        if (projectsPrevBtn != null) projectsPrevBtn.setDisable(projectsCurrentPage <= 1);
        if (projectsNextBtn != null) projectsNextBtn.setDisable(projectsCurrentPage >= total);
    }

    private void setupSubmissionsTable() {
        if (submissionsList == null) return;
        submissionsList.setItems(submissions);
        submissionsList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ProjectSubmissionView s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label title = new Label(safe(s.getProjectTitle()));
                title.getStyleClass().add("stat-value");
                Label meta = new Label(safe(s.getSubmittedAt()) + "  •  " + safe(s.getStudentEmail()));
                meta.getStyleClass().add("page-subtitle");
                Label resp = new Label(summarize(safe(s.getTextResponse()), 120));
                resp.setWrapText(true);
                Button dlC = new Button("Télécharger"); dlC.getStyleClass().add("btn-rgb-compact"); dlC.setDisable(safe(s.getCahierPath()).isBlank()); dlC.setOnAction(e -> { try { File f = new File(s.getCahierPath()); if (f.exists()) Desktop.getDesktop().open(f); else info("Fichier introuvable","Le fichier n'existe pas: " + s.getCahierPath()); } catch (Exception ex){ error("Erreur ouverture fichier", ex); } });
                Button dlZ = new Button("Télécharger"); dlZ.getStyleClass().add("btn-rgb-compact"); dlZ.setDisable(safe(s.getDossierPath()).isBlank()); dlZ.setOnAction(e -> { try { File f = new File(s.getDossierPath()); if (f.exists()) Desktop.getDesktop().open(f); else info("Fichier introuvable","Le fichier n'existe pas: " + s.getDossierPath()); } catch (Exception ex){ error("Erreur ouverture fichier", ex); } });
                Button kan = new Button("Voir Kanban"); kan.getStyleClass().add("btn-rgb-outline"); kan.setOnAction(e -> { selectedSubmission = s; openKanbanWindow(s); });
                HBox right = new HBox(8, dlC, dlZ, kan);
                VBox left = new VBox(4, title, meta, resp);
                Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox row = new HBox(12, left, spacer, right); row.getStyleClass().add("submission-row");
                setGraphic(row); setText(null);
            }
        });
        submissionsList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> selectedSubmission = newV);
        if (submissionsSearchField != null) submissionsSearchField.textProperty().addListener((obs, oldV, newV) -> applySubmissionsFilterAndSort());
        if (submissionsSortCombo != null) {
            submissionsSortCombo.getItems().setAll("Date desc", "Date asc", "Projet A-Z", "Projet Z-A");
            if (submissionsSortCombo.getValue() == null || String.valueOf(submissionsSortCombo.getValue()).isBlank()) submissionsSortCombo.setValue("Date desc");
            submissionsSortCombo.valueProperty().addListener((obs, oldV, newV) -> applySubmissionsFilterAndSort());
        }
    }

    private void openKanbanWindow(ProjectSubmissionView submission) {
        if (submission == null) {
            return;
        }
        try {
            var url = getClass().getResource("/View/back/BackKanban.fxml");
            if (url == null) {
                info("Kanban", "Vue Kanban introuvable (BackKanban.fxml).");
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            BackKanbanController controller = loader.getController();
            if (controller != null) {
                controller.setSubmission(submission);
            }

            Scene ownerScene = submissionsList == null ? null : submissionsList.getScene();
            Window owner = ownerScene == null ? null : ownerScene.getWindow();

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) {
                stage.initOwner(owner);
            }

            Scene scene = new Scene(root, 980, 640);
            if (ownerScene != null) {
                scene.getStylesheets().setAll(ownerScene.getStylesheets());
            }
            Theme.apply(root);
            stage.setScene(scene);
            stage.setTitle("Kanban");
            stage.show();
        } catch (Exception e) {
            error("Erreur Kanban", e);
        }
    }

    @FXML
    private void reloadProjects(ActionEvent event) {
        String q = projectSearchField == null ? "" : String.valueOf(projectSearchField.getText());
        allProjects.clear();
        allProjects.addAll(projectRepo.listAll(q));
        refreshSubmissionCounts();
        applyProjectsSort();
        // refresh admin cards view when projects change — render from full list
        renderAdminCards();
        // show first page
        projectsCurrentPage = 1;
        showProjectsPage(projectsCurrentPage);
    }

    @FXML
    private void editProject(ActionEvent event) {
        Project sel = projectsList == null ? null : projectsList.getSelectionModel().getSelectedItem();
        if (sel == null) {
            info("Modification", "Sélectionnez un projet à modifier.");
            return;
        }
        openProjectDialog(sel);
    }

    @FXML
    private void newProject(ActionEvent event) {
        openProjectDialog(null);
    }

    @FXML
    private void saveProject(ActionEvent event) {
        Project p = editingProject == null ? new Project() : editingProject;
        p.setTitle(text(titleField));

        // build deadline string from DatePicker + time field
        if (deadlineDatePicker != null && deadlineTimeField != null) {
            java.time.LocalDate ld = deadlineDatePicker.getValue();
            String timePart = safe(deadlineTimeField.getText());
            if (ld == null) {
                p.setDeadline("");
            } else {
                if (timePart.isBlank()) timePart = "00:00:00";
                p.setDeadline(ld.toString() + " " + timePart);
            }
        } else {
            p.setDeadline("");
        }
        p.setDeliverables(text(deliverablesArea));
        p.setDescription(text(descriptionArea));
        p.setPublished(publishedCheck != null && publishedCheck.isSelected());

        // ── Validation logique via service ──
        ValidationResult result = ProjectValidationService.validateProject(p);

        // Marquer les champs en erreur visuellement
        FormValidator fv = new FormValidator();
        fv.check(titleField, ProjectValidationService.validateTitreProjet(p.getTitle()));
        ValidationResult dlR = ProjectValidationService.validateDeadlineStr(p.getDeadline());
        if (deadlineDatePicker != null) {
            fv.check(deadlineDatePicker, dlR);
            if (deadlineError != null) {
                boolean has = !dlR.isValid();
                deadlineError.setVisible(has);
                deadlineError.setManaged(has);
                deadlineError.setText(has ? String.join(" ", dlR.getErrors()) : "");
            }
        }
        if (descriptionArea != null) {
            ValidationResult descR = new ValidationResult();
            String desc = p.getDescription();
            if (desc == null || desc.isBlank()) {
                descR.addError("La description est obligatoire.");
            } else if (desc.trim().length() < 10) {
                descR.addError("La description doit contenir au moins 10 caractères.");
            }
            fv.check(descriptionArea, descR);
            if (descriptionError != null) {
                boolean has = !descR.isValid();
                descriptionError.setVisible(has);
                descriptionError.setManaged(has);
                descriptionError.setText(has ? String.join(" ", descR.getErrors()) : "");
            }
        }

        if (!result.isValid()) {
            FormValidator.showErrorAlert("Projet invalide", result);
            return;
        }

        try {
            if (p.getId() <= 0) {
                p.setCreatedById(AppState.getUserId());
                projectRepo.create(p);
                projects.add(0, p);
                if (projectsList != null) projectsList.getSelectionModel().select(p);
                renderAdminCards();
            } else {
                projectRepo.update(p);
                if (projectsList != null) projectsList.refresh();
                renderAdminCards();
            }
            info("Projet enregistré", "Le projet a été enregistré.");
            newProject(null);
        } catch (Exception e) {
            error("Erreur", e);
        }
    }

    @FXML
    private void deleteProject(ActionEvent event) {
        Project sel = projectsList == null ? null : projectsList.getSelectionModel().getSelectedItem();
        if (sel == null) {
            info("Suppression", "Sélectionnez un projet à supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer le projet ?");
        confirm.setContentText("Projet: " + safe(sel.getTitle()) + " (ID " + sel.getId() + ")");
        var res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }

        try {
            projectRepo.delete(sel.getId());
            info("Suppression", "Projet supprimé.");
            reloadProjects(null);
        } catch (Exception e) {
            error("Erreur", e);
        }
    }

    private void fillProjectForm(Project p) {
        if (titleField != null) {
            titleField.setText(p == null ? "" : safe(p.getTitle()));
        }
        // populate date and time fields from deadline string if present
        if (deadlineDatePicker != null && deadlineTimeField != null) {
            if (p == null || safe(p.getDeadline()).isBlank()) {
                deadlineDatePicker.setValue(null);
                deadlineTimeField.setText("");
            } else {
                String dl = safe(p.getDeadline());
                String datePart = dl.length() >= 10 ? dl.substring(0, 10) : dl;
                try {
                    deadlineDatePicker.setValue(java.time.LocalDate.parse(datePart));
                } catch (Exception ignore) {
                    deadlineDatePicker.setValue(null);
                }
                if (dl.length() > 11) {
                    String timePart = dl.length() >= 19 ? dl.substring(11, 19) : dl.substring(11);
                    deadlineTimeField.setText(timePart);
                } else {
                    deadlineTimeField.setText("");
                }
            }
        }
        if (deliverablesArea != null) {
            deliverablesArea.setText(p == null ? "" : safe(p.getDeliverables()));
        }
        if (descriptionArea != null) {
            descriptionArea.setText(p == null ? "" : safe(p.getDescription()));
        }
        if (publishedCheck != null) {
            publishedCheck.setSelected(p != null && p.isPublished());
        }
    }

    private void openProjectDialog(Project source) {
        FormResult<Project> result = showProjectForm(source);
        if (!result.saved()) return;

        try {
            Project project = result.value();
            if (project.getId() <= 0) {
                project.setCreatedById(AppState.getUserId());
                projectRepo.create(project);
                info("Projet enregistré", "Le projet a été ajouté.");
            } else {
                projectRepo.update(project);
                info("Projet enregistré", "Le projet a été mis à jour.");
            }
            reloadProjects(null);
            selectProjectById(project.getId());
        } catch (Exception e) {
            error("Erreur", e);
        }
    }

    private FormResult<Project> showProjectForm(Project source) {
        TextField title = field();
        DatePicker datePicker = new DatePicker();
        datePicker.getStyleClass().addAll("field", "date-picker");
        datePicker.setMaxWidth(Double.MAX_VALUE);

        TextField timeField = field();
        timeField.setPromptText("HH:mm:ss");
        TextArea description = area();
        CheckBox published = new CheckBox("Publié");

        if (source != null) {
            title.setText(safe(source.getTitle()));
            description.setText(safe(source.getDescription()));
            published.setSelected(source.isPublished());
            fillDeadlineFields(source.getDeadline(), datePicker, timeField);
        }

        GridPane grid = formGrid();
        Label titleErr = addFormRow(grid, 0, "Titre *", title);
        HBox deadlineBox = new HBox(8, datePicker, timeField);
        HBox.setHgrow(datePicker, Priority.ALWAYS);
        HBox.setHgrow(timeField, Priority.ALWAYS);
        Label deadlineErr = addFormRow(grid, 1, "Deadline *", deadlineBox);
        Label descErr = addFormRow(grid, 2, "Description *", description);
        Label publishedErr = addFormRow(grid, 3, "Publication", published);
        publishedErr.setText("");

        liveValidate(title, titleErr, () -> ProjectValidationService.validateTitreProjet(title.getText()));
        datePicker.valueProperty().addListener((obs, o, n) -> validateDeadlineFields(datePicker, timeField, deadlineErr));
        timeField.textProperty().addListener((obs, o, n) -> validateDeadlineFields(datePicker, timeField, deadlineErr));
        liveValidate(description, descErr, () -> validateProjectDescription(description.getText()));

        Dialog<ButtonType> dialog = buildFormDialog(source == null ? "Créer un projet" : "Modifier un projet", grid);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            ValidationResult titleResult = ProjectValidationService.validateTitreProjet(title.getText());
            ValidationResult deadlineResult = ProjectValidationService.validateDeadlineStr(buildDeadline(datePicker, timeField));
            ValidationResult descResult = validateProjectDescription(description.getText());

            applyValidation(title, titleErr, titleResult);
            applyValidation(datePicker, deadlineErr, deadlineResult);
            if (deadlineResult.isValid()) FormValidator.clearError(timeField);
            else FormValidator.markError(timeField, deadlineResult.firstError());
            applyValidation(description, descErr, descResult);

            if (!titleResult.isValid() || !deadlineResult.isValid() || !descResult.isValid()) {
                ev.consume();
            }
        });

        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return FormResult.cancelled();
        }

        Project project = source == null ? new Project() : source;
        project.setTitle(text(title));
        project.setDeadline(buildDeadline(datePicker, timeField));
        project.setDescription(text(description));
        project.setPublished(published.isSelected());
        project.setDeliverables(source == null ? "" : safe(source.getDeliverables()));
        return FormResult.saved(project);
    }

    @FXML
    private void reloadSubmissions(ActionEvent event) {
        allSubmissions.setAll(submissionRepo.listAll());
        refreshSubmissionCounts();
        applySubmissionsFilterAndSort();
        if (!submissions.isEmpty() && submissionsList != null && submissionsList.getSelectionModel().getSelectedItem() == null) {
            submissionsList.getSelectionModel().selectFirst();
        }
    }

    private void refreshSubmissionCounts() {
        try {
            Map<Integer, Integer> m = submissionRepo.countByProject();
            submissionsCountByProject.clear();
            if (m != null) {
                submissionsCountByProject.putAll(m);
            }
            if (projectsList != null) {
                projectsList.refresh();
            }
        } catch (Exception ignore) {
        }
    }

    private void applyProjectsSort() {
        String sort = projectsSortCombo == null ? "Date desc" : String.valueOf(projectsSortCombo.getValue());
        if ("Projet A-Z".equalsIgnoreCase(sort) || "Titre A-Z".equalsIgnoreCase(sort)) {
            allProjects.sort(Comparator.comparing((Project p) -> safe(p == null ? null : p.getTitle()), String.CASE_INSENSITIVE_ORDER));
        } else if ("Projet Z-A".equalsIgnoreCase(sort)) {
            allProjects.sort(Comparator.comparing((Project p) -> safe(p == null ? null : p.getTitle()), String.CASE_INSENSITIVE_ORDER).reversed());
        } else if ("Deadline".equalsIgnoreCase(sort)) {
            allProjects.sort(Comparator.comparing((Project p) -> safe(p == null ? null : p.getDeadline())));
        } else if ("Date asc".equalsIgnoreCase(sort)) {
            allProjects.sort(Comparator.comparing((Project p) -> safe(p == null ? null : p.getCreatedAt())));
        } else {
            allProjects.sort(Comparator.comparing((Project p) -> safe(p == null ? null : p.getCreatedAt())).reversed());
        }
        // refresh current page view after sorting
        showProjectsPage(projectsCurrentPage <= 0 ? 1 : projectsCurrentPage);
    }

    private static LocalDate parseIsoDate(String value) {
        String v = safe(value);
        if (v.length() >= 10) {
            v = v.substring(0, 10);
        }
        if (v.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void applySubmissionsFilterAndSort() {
        String q = submissionsSearchField == null ? "" : String.valueOf(submissionsSearchField.getText()).trim().toLowerCase();
        List<ProjectSubmissionView> filtered = new ArrayList<>();
        for (ProjectSubmissionView s : allSubmissions) {
            if (s == null) continue;
            String project = safe(s.getProjectTitle()).toLowerCase();
            String student = safe(s.getStudentEmail()).toLowerCase();
            String resp = safe(s.getTextResponse()).toLowerCase();
            if (q.isBlank() || project.contains(q) || student.contains(q) || resp.contains(q)) {
                filtered.add(s);
            }
        }
        String sort = submissionsSortCombo == null ? "Date desc" : String.valueOf(submissionsSortCombo.getValue());
        if ("Projet Z-A".equalsIgnoreCase(sort)) {
            filtered.sort(Comparator.comparing((ProjectSubmissionView s) -> safe(s.getProjectTitle()), String.CASE_INSENSITIVE_ORDER).reversed());
        } else if ("Projet A-Z".equalsIgnoreCase(sort)) {
            filtered.sort(Comparator.comparing((ProjectSubmissionView s) -> safe(s.getProjectTitle()), String.CASE_INSENSITIVE_ORDER));
        } else if ("Date asc".equalsIgnoreCase(sort)) {
            filtered.sort(Comparator.comparing((ProjectSubmissionView s) -> safe(s.getSubmittedAt())));
        } else {
            filtered.sort((a, b) -> safe(b.getSubmittedAt()).compareTo(safe(a.getSubmittedAt())));
        }
        submissions.setAll(filtered);
    }

    private Dialog<ButtonType> buildFormDialog(String title, Node content) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(760);
        styleDialog(dialog);
        return dialog;
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(4);
        return grid;
    }

    private Label addFormRow(GridPane grid, int row, String label, Node node) {
        Label formLabel = new Label(label);
        formLabel.getStyleClass().add("form-label");
        grid.add(formLabel, 0, row * 2);
        grid.add(node, 1, row * 2);
        if (node instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(region, Priority.ALWAYS);
        }
        Label err = new Label();
        err.setStyle("-fx-text-fill: #d6293e; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 0 0 4 2;");
        err.setWrapText(true);
        err.setMaxWidth(440);
        grid.add(err, 1, row * 2 + 1);
        return err;
    }

    private void liveValidate(TextInputControl field, Label errLabel, java.util.function.Supplier<ValidationResult> validator) {
        field.textProperty().addListener((obs, o, n) -> applyValidation(field, errLabel, validator.get()));
    }

    private void validateDeadlineFields(DatePicker datePicker, TextField timeField, Label errLabel) {
        ValidationResult result = ProjectValidationService.validateDeadlineStr(buildDeadline(datePicker, timeField));
        applyValidation(datePicker, errLabel, result);
        if (result.isValid()) FormValidator.clearError(timeField);
        else FormValidator.markError(timeField, result.firstError());
    }

    private void applyValidation(javafx.scene.control.Control field, Label errLabel, ValidationResult result) {
        if (result == null || result.isValid()) {
            errLabel.setText("");
            FormValidator.clearError(field);
        } else {
            errLabel.setText("⚠ " + result.firstError());
            FormValidator.markError(field, result.firstError());
        }
    }

    private ValidationResult validateProjectDescription(String value) {
        ValidationResult result = new ValidationResult();
        String text = safe(value);
        if (text.isBlank()) result.addError("La description est obligatoire.");
        else if (text.length() < 10) result.addError("La description doit contenir au moins 10 caractères.");
        else if (text.length() > 3000) result.addError("La description ne doit pas dépasser 3000 caractères.");
        return result;
    }

    private void fillDeadlineFields(String deadline, DatePicker datePicker, TextField timeField) {
        String value = safe(deadline);
        if (value.isBlank()) {
            datePicker.setValue(null);
            timeField.setText("");
            return;
        }
        String datePart = value.length() >= 10 ? value.substring(0, 10) : value;
        try {
            datePicker.setValue(LocalDate.parse(datePart));
        } catch (Exception e) {
            datePicker.setValue(null);
        }
        timeField.setText(value.length() > 11 ? value.substring(11) : "");
    }

    private String buildDeadline(DatePicker datePicker, TextField timeField) {
        LocalDate date = datePicker == null ? null : datePicker.getValue();
        if (date == null) return "";
        String time = timeField == null ? "" : safe(timeField.getText());
        if (time.isBlank()) time = "00:00:00";
        return date + " " + time;
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

    private void selectProjectById(int projectId) {
        if (projectId <= 0) return;
        int index = -1;
        for (int i = 0; i < allProjects.size(); i++) {
            if (allProjects.get(i).getId() == projectId) {
                index = i;
                break;
            }
        }
        if (index < 0) return;
        int page = (index / projectsPageSize) + 1;
        showProjectsPage(page);
        for (Project project : projects) {
            if (project.getId() == projectId) {
                projectsList.getSelectionModel().select(project);
                projectsList.scrollTo(project);
                editingProject = project;
                return;
            }
        }
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
        // blue RGB icon
        Label icon = new Label("✓");
        icon.setStyle("-fx-text-fill: white; -fx-background-color: linear-gradient(to right, rgba(0,210,255,0.98), rgba(6,106,201,0.98)); -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-background-radius: 20; -fx-font-size: 14px;");
        a.getDialogPane().setGraphic(icon);
        styleDialog(a);
        a.showAndWait();
    }

    private static void error(String title, Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(e == null ? "Erreur" : String.valueOf(e.getMessage()));
        Label icon = new Label("✕");
        icon.setStyle("-fx-text-fill: white; -fx-background-color: #e53935; -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-background-radius: 20; -fx-font-size: 14px;");
        a.getDialogPane().setGraphic(icon);
        styleDialog(a);
        a.showAndWait();
        if (e != null) {
            e.printStackTrace();
        }
    }

    private static void styleDialog(javafx.scene.control.Dialog<?> d) {
        if (d == null || d.getDialogPane() == null) return;
        String css = cssUri();
        if (!css.isBlank() && !d.getDialogPane().getStylesheets().contains(css)) {
            d.getDialogPane().getStylesheets().add(css);
        }
        if (!d.getDialogPane().getStyleClass().contains("rgb-dialog")) {
            d.getDialogPane().getStyleClass().add("rgb-dialog");
        }
        for (javafx.scene.control.ButtonType bt : d.getDialogPane().getButtonTypes()) {
            javafx.scene.Node b = d.getDialogPane().lookupButton(bt);
            if (b == null) continue;
            if (bt == javafx.scene.control.ButtonType.OK) {
                b.getStyleClass().add("btn-rgb");
            } else if (bt == javafx.scene.control.ButtonType.CANCEL) {
                b.getStyleClass().add("btn-rgb-outline");
            }
        }
    }

    private static String cssUri() {
        java.io.File f = new java.io.File("styles/educompus.css");
        if (!f.exists()) {
            f = new java.io.File("eduCompus-javafx/styles/educompus.css");
        }
        if (!f.exists()) {
            f = new java.io.File(new java.io.File("..", "eduCompus-javafx"), "styles/educompus.css");
        }
        return f.exists() ? f.toURI().toString() : "";
    }

    private static boolean containsDigit(String value) {
        for (char c : safe(value).toCharArray()) {
            if (Character.isDigit(c)) return true;
        }
        return false;
    }

    private record FormResult<T>(T value, boolean saved) {
        static <T> FormResult<T> saved(T value) { return new FormResult<>(value, true); }
        static <T> FormResult<T> cancelled() { return new FormResult<>(null, false); }
    }
}

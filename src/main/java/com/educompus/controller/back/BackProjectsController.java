package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.model.Project;
import com.educompus.model.ProjectSubmissionView;
import com.educompus.repository.ProjectRepository;
import com.educompus.repository.ProjectSubmissionRepository;
import com.educompus.service.FormValidator;
import com.educompus.service.ProjectValidationService;
import com.educompus.service.ValidationResult;
import com.educompus.util.Theme;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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

public final class BackProjectsController {
    private final ProjectRepository projectRepo = new ProjectRepository();
    private final ProjectSubmissionRepository submissionRepo = new ProjectSubmissionRepository();

    @FXML
    private MenuButton projectsMenuButton;

    @FXML
    private javafx.scene.control.MenuItem catalogueMenuItem;

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
    private TableView<Project> projectsTable;

    @FXML
    private TableColumn<Project, Integer> colProjectId;

    @FXML
    private TableColumn<Project, String> colProjectTitle;

    @FXML
    private TableColumn<Project, String> colProjectDeadline;

    @FXML
    private TableColumn<Project, Boolean> colProjectPublished;

    @FXML
    private TableColumn<Project, String> colProjectCreatedAt;

    @FXML
    private TableColumn<Project, Integer> colProjectSubmissions;

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
    private CheckBox publishedCheck;

    @FXML
    private TableView<ProjectSubmissionView> submissionsTable;

    @FXML
    private TableColumn<ProjectSubmissionView, Integer> colSubId;

    @FXML
    private TableColumn<ProjectSubmissionView, String> colSubProject;

    @FXML
    private TableColumn<ProjectSubmissionView, String> colSubStudent;

    @FXML
    private TableColumn<ProjectSubmissionView, String> colSubSubmittedAt;

    @FXML
    private TableColumn<ProjectSubmissionView, Void> colSubActions;

    @FXML
    private TableColumn<ProjectSubmissionView, String> colSubResponse;

    @FXML
    private TableColumn<ProjectSubmissionView, String> colSubFile;

    @FXML
    private TableColumn<ProjectSubmissionView, String> colSubZip;

    @FXML
    private TextField submissionsSearchField;

    @FXML
    private ComboBox<String> submissionsSortCombo;

    @FXML
    private FlowPane adminCardsFlow;

    private final ObservableList<Project> projects = FXCollections.observableArrayList();
    private final ObservableList<ProjectSubmissionView> submissions = FXCollections.observableArrayList();
    private final ObservableList<ProjectSubmissionView> allSubmissions = FXCollections.observableArrayList();
    private final Map<Integer, Integer> submissionsCountByProject = new HashMap<>();

    private Project editingProject;
    private ProjectSubmissionView selectedSubmission;

    @FXML
    private void initialize() {
        boolean adminOnlyCatalogue = AppState.isAdmin();
        if (catalogueMenuItem != null) {
            catalogueMenuItem.setVisible(adminOnlyCatalogue);
            catalogueMenuItem.setDisable(!adminOnlyCatalogue);
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
            projectsSortCombo.getItems().setAll("Date desc", "Date asc", "Titre A-Z", "Deadline");
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
        if (!AppState.isAdmin()) {
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
        if (!AppState.isAdmin() || adminCardsFlow == null) return;
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

        for (Project p : projects) {
            if (p == null) continue;
            VBox card = buildAdminProjectCard(p);
            card.setPrefWidth(cardW);
            card.setMinWidth(180);
            adminCardsFlow.getChildren().add(card);
        }
    }

    private VBox buildAdminProjectCard(Project project) {
        VBox card = new VBox(10);
        card.getStyleClass().add("project-card");
        card.setPadding(new javafx.geometry.Insets(14));
        card.setPrefWidth(260);
        card.setMinWidth(240);

        HBox top = new HBox(10);
        Label title = new Label(safe(project.getTitle()));
        title.getStyleClass().add("project-card-title");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label chip = new Label(safe(project.getDeadline()));
        chip.getStyleClass().addAll("chip", "chip-info");
        // keep the deadline chip compact so it doesn't stretch across the card
        chip.setWrapText(false);
        chip.setPrefWidth(140);
        chip.setMaxWidth(220);
        chip.setMinWidth(Region.USE_PREF_SIZE);

        // populate the top row with title + deadline chip
        top.getChildren().addAll(title, chip);

        // published badge: show emoji in top-right (✅ / ❌)
        boolean published = project.isPublished();
        Label pubBadge = new Label(published ? "✅" : "❌");
        pubBadge.getStyleClass().addAll("pub-emoji");
        pubBadge.setStyle("-fx-font-size:18px; -fx-background-color: transparent; -fx-padding: 4 6 4 6;");
        javafx.scene.control.Tooltip.install(pubBadge, new javafx.scene.control.Tooltip(published ? "Publié" : "Non publié"));

        // build card content and subtitle (description)
        String desc = safe(project.getDescription());
        Label subtitle = new Label(desc.isBlank() ? "Aucune description" : summarize(desc, 110));
        subtitle.getStyleClass().add("project-card-subtitle");
        subtitle.setWrapText(true);

        VBox content = new VBox(8);
        content.getChildren().addAll(top, subtitle);

        // container stack so we can place the badge in the top-right corner
        StackPane container = new StackPane();
        container.getChildren().add(content);
        StackPane.setAlignment(pubBadge, javafx.geometry.Pos.TOP_RIGHT);
        StackPane.setMargin(pubBadge, new javafx.geometry.Insets(8, 8, 0, 0));
        container.getChildren().add(pubBadge);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);

        // removed direct Voir/Éditer buttons from card per admin design

        // publish/unpublish button (avoid aggressive red style; use RGB outline when published)
        Button btnPublish = new Button(published ? "Dépublier" : "Publier");
        btnPublish.getStyleClass().add(published ? "btn-rgb-outline" : "btn-rgb");
        btnPublish.setOnAction(e -> {
            // confirmation before toggling
            String action = project.isPublished() ? "dépublier" : "publier";
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText((project.isPublished() ? "Dépublier" : "Publier") + " le projet ?");
            confirm.setContentText("Projet: " + safe(project.getTitle()) + "\nVoulez-vous vraiment " + action + " ce projet ?");
            var res = confirm.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) {
                return;
            }
            try {
                project.setPublished(!project.isPublished());
                projectRepo.update(project);
                info("Publication", project.isPublished() ? "Projet publié." : "Projet dépublié.");
                // refresh UI
                if (projectsTable != null) projectsTable.refresh();
                renderAdminCards();
            } catch (Exception ex) {
                error("Erreur", ex);
            }
        });

        HBox actions = new HBox(10, grow, btnPublish);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        card.getChildren().addAll(container, actions);
        return card;
    }

    private void setupProjectsTable() {
        if (projectsTable == null) {
            return;
        }
        projectsTable.setItems(projects);

        if (colProjectId != null) {
            colProjectId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
            colProjectId.setVisible(false); // hide ID column in admin list as requested
        }
        if (colProjectTitle != null) {
            colProjectTitle.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));
        }
        if (colProjectDeadline != null) {
            colProjectDeadline.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("deadline"));
        }
        if (colProjectPublished != null) {
            colProjectPublished.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("published"));
            colProjectPublished.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : (Boolean.TRUE.equals(item) ? "Oui" : "Non"));
                }
            });
        }
        if (colProjectCreatedAt != null) {
            colProjectCreatedAt.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("createdAt"));
        }
        if (colProjectSubmissions != null) {
            colProjectSubmissions.setCellValueFactory(c -> {
                Project p = c == null ? null : c.getValue();
                int id = p == null ? 0 : p.getId();
                int count = id <= 0 ? 0 : submissionsCountByProject.getOrDefault(id, 0);
                return new javafx.beans.property.SimpleObjectProperty<>(count);
            });
        }

        projectsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            editingProject = newV;
            fillProjectForm(newV);
        });

        if (projectSearchField != null) {
            projectSearchField.setOnAction(e -> reloadProjects(null));
        }
    }

    private void setupSubmissionsTable() {
        if (submissionsTable == null) {
            return;
        }
        submissionsTable.setItems(submissions);

        if (colSubId != null) {
            colSubId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
            colSubId.setVisible(false); // hide id column
        }
        if (colSubProject != null) {
            colSubProject.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue() == null ? null : c.getValue().getProjectTitle())));
        }
        if (colSubStudent != null) {
            colSubStudent.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue() == null ? null : c.getValue().getStudentEmail())));
        }
        if (colSubSubmittedAt != null) {
            colSubSubmittedAt.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("submittedAt"));
        }

        if (colSubResponse != null) {
            colSubResponse.setCellValueFactory(c -> new SimpleStringProperty(summarize(safe(c.getValue() == null ? null : c.getValue().getTextResponse()), 120)));
        }

        if (colSubFile != null) {
            colSubFile.setCellFactory(col -> new TableCell<>() {
                private final Button btn = new Button("Télécharger");
                {
                    btn.getStyleClass().add("btn-rgb-compact");
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    ProjectSubmissionView s = getTableView().getItems().get(getIndex());
                    String path = s == null ? null : s.getCahierPath();
                    if (path == null || path.isBlank()) {
                        btn.setDisable(true);
                    } else {
                        btn.setDisable(false);
                        btn.setOnAction(e -> {
                            try {
                                File f = new File(path);
                                if (f.exists()) {
                                    Desktop.getDesktop().open(f);
                                } else {
                                    info("Fichier introuvable", "Le fichier n'existe pas: " + path);
                                }
                            } catch (Exception ex) {
                                error("Erreur ouverture fichier", ex);
                            }
                        });
                    }
                    setGraphic(btn);
                    setText(null);
                }
            });
        }

        if (colSubZip != null) {
            colSubZip.setCellFactory(col -> new TableCell<>() {
                private final Button btn = new Button("Télécharger");
                {
                    btn.getStyleClass().add("btn-rgb-compact");
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    ProjectSubmissionView s = getTableView().getItems().get(getIndex());
                    String path = s == null ? null : s.getDossierPath();
                    if (path == null || path.isBlank()) {
                        btn.setDisable(true);
                    } else {
                        btn.setDisable(false);
                        btn.setOnAction(e -> {
                            try {
                                File f = new File(path);
                                if (f.exists()) {
                                    Desktop.getDesktop().open(f);
                                } else {
                                    info("Fichier introuvable", "Le fichier n'existe pas: " + path);
                                }
                            } catch (Exception ex) {
                                error("Erreur ouverture fichier", ex);
                            }
                        });
                    }
                    setGraphic(btn);
                    setText(null);
                }
            });
        }

        if (colSubActions != null) {
            colSubActions.setCellFactory(col -> new TableCell<>() {
                private final Button btn = new Button("Voir Kanban");
                {
                    btn.getStyleClass().add("btn-rgb-outline");
                    btn.setOnAction(e -> {
                        int idx = getIndex();
                        if (idx < 0 || idx >= submissionsTable.getItems().size()) return;
                        ProjectSubmissionView s = submissionsTable.getItems().get(idx);
                        if (s == null) return;
                        submissionsTable.getSelectionModel().select(s);
                        selectedSubmission = s;
                        openKanbanWindow(s);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    setGraphic(btn);
                    setText(null);
                }
            });
        }

        submissionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedSubmission = newV;
        });

        if (submissionsSearchField != null) {
            submissionsSearchField.textProperty().addListener((obs, oldV, newV) -> applySubmissionsFilterAndSort());
        }
        if (submissionsSortCombo != null) {
            submissionsSortCombo.getItems().setAll("Date desc", "Date asc", "Projet A-Z", "Projet Z-A");
            if (submissionsSortCombo.getValue() == null || String.valueOf(submissionsSortCombo.getValue()).isBlank()) {
                submissionsSortCombo.setValue("Date desc");
            }
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

            Scene ownerScene = submissionsTable == null ? null : submissionsTable.getScene();
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
        projects.setAll(projectRepo.listAll(q));
        refreshSubmissionCounts();
        applyProjectsSort();
        // refresh admin cards view when projects change
        renderAdminCards();
        if (!projects.isEmpty() && projectsTable != null && projectsTable.getSelectionModel().getSelectedItem() == null) {
            projectsTable.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void editProject(ActionEvent event) {
        Project sel = projectsTable == null ? null : projectsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            info("Modification", "Sélectionnez un projet à modifier.");
            return;
        }
        editingProject = sel;
        fillProjectForm(sel);
        if (titleField != null) {
            titleField.requestFocus();
            titleField.selectAll();
        }
    }

    @FXML
    private void newProject(ActionEvent event) {
        editingProject = null;
        if (projectsTable != null) {
            projectsTable.getSelectionModel().clearSelection();
        }
        fillProjectForm(null);
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
        if (deadlineDatePicker != null) {
            fv.check(deadlineDatePicker, ProjectValidationService.validateDeadlineStr(p.getDeadline()));
        }
        if (descriptionArea != null) {
            ValidationResult descR = new ValidationResult();
            // description optionnelle mais si renseignée doit avoir du sens
            String desc = p.getDescription();
            if (desc != null && !desc.isBlank() && desc.trim().length() < 10) {
                descR.addError("La description doit contenir au moins 10 caractères si elle est renseignée.");
            }
            fv.check(descriptionArea, descR);
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
                if (projectsTable != null) projectsTable.getSelectionModel().select(p);
                renderAdminCards();
            } else {
                projectRepo.update(p);
                if (projectsTable != null) projectsTable.refresh();
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
        Project sel = projectsTable == null ? null : projectsTable.getSelectionModel().getSelectedItem();
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
            projects.remove(sel);
            // update admin catalogue after deletion
            renderAdminCards();
            newProject(null);
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

    @FXML
    private void reloadSubmissions(ActionEvent event) {
        allSubmissions.setAll(submissionRepo.listAll());
        refreshSubmissionCounts();
        applySubmissionsFilterAndSort();
        if (!submissions.isEmpty() && submissionsTable != null && submissionsTable.getSelectionModel().getSelectedItem() == null) {
            submissionsTable.getSelectionModel().selectFirst();
        }
    }

    private void refreshSubmissionCounts() {
        try {
            Map<Integer, Integer> m = submissionRepo.countByProject();
            submissionsCountByProject.clear();
            if (m != null) {
                submissionsCountByProject.putAll(m);
            }
            if (projectsTable != null) {
                projectsTable.refresh();
            }
        } catch (Exception ignore) {
        }
    }

    private void applyProjectsSort() {
        String sort = projectsSortCombo == null ? "Date desc" : String.valueOf(projectsSortCombo.getValue());
        if ("Titre A-Z".equalsIgnoreCase(sort)) {
            projects.sort(Comparator.comparing((Project p) -> safe(p == null ? null : p.getTitle()), String.CASE_INSENSITIVE_ORDER));
        } else if ("Deadline".equalsIgnoreCase(sort)) {
            projects.sort(Comparator.comparing((Project p) -> safe(p == null ? null : p.getDeadline())));
        } else if ("Date asc".equalsIgnoreCase(sort)) {
            projects.sort(Comparator.comparing((Project p) -> safe(p == null ? null : p.getCreatedAt())));
        } else {
            projects.sort(Comparator.comparing((Project p) -> safe(p == null ? null : p.getCreatedAt())).reversed());
        }
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
}

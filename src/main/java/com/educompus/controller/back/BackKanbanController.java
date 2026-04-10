package com.educompus.controller.back;

import com.educompus.model.KanbanStatus;
import com.educompus.model.KanbanTask;
import com.educompus.model.ProjectSubmissionView;
import com.educompus.repository.KanbanTaskRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BackKanbanController {
    private final KanbanTaskRepository kanbanRepo = new KanbanTaskRepository();

    @FXML
    private Label headerLabel;

    @FXML
    private TextField searchField;

    @FXML
    private VBox todoColumn;

    @FXML
    private VBox inProgressColumn;

    @FXML
    private VBox doneColumn;

    private ProjectSubmissionView submission;
    private final List<KanbanTask> allTasks = new ArrayList<>();

    @FXML
    private void initialize() {
        renderKanbanColumns(List.of());
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> renderFiltered());
        }
        setupDnD();
    }

    private void setupDnD() {
        setupDropZone(todoColumn, KanbanStatus.TODO);
        setupDropZone(inProgressColumn, KanbanStatus.IN_PROGRESS);
        setupDropZone(doneColumn, KanbanStatus.DONE);
    }

    private void setupDropZone(VBox column, KanbanStatus target) {
        if (column == null || target == null) {
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
                    reload(null);
                    ok = true;
                } catch (Exception ex) {
                    error("Erreur", ex);
                }
            }
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    public void setSubmission(ProjectSubmissionView submission) {
        this.submission = submission;
        updateHeader();
        reload(null);
    }

    @FXML
    private void reload(javafx.event.ActionEvent event) {
        if (submission == null) {
            allTasks.clear();
            renderKanbanColumns(List.of());
            return;
        }
        try {
            List<KanbanTask> list = kanbanRepo.listByProjectAndStudent(submission.getProjectId(), submission.getStudentId());
            allTasks.clear();
            allTasks.addAll(list);
            renderFiltered();
        } catch (Exception e) {
            error("Erreur Kanban", e);
        }
    }

    @FXML
    private void close(javafx.event.ActionEvent event) {
        Window w = headerLabel == null ? null : headerLabel.getScene() == null ? null : headerLabel.getScene().getWindow();
        if (w != null) {
            w.hide();
        }
    }

    private void updateHeader() {
        if (headerLabel == null) {
            return;
        }
        if (submission == null) {
            headerLabel.setText("Kanban");
            return;
        }
        String p = safe(submission.getProjectTitle());
        String u = safe(submission.getStudentEmail());
        String left = p.isBlank() ? ("Projet #" + submission.getProjectId()) : p;
        String right = u.isBlank() ? ("Étudiant #" + submission.getStudentId()) : u;
        headerLabel.setText("Kanban — " + left + " — " + right);
    }

    private void renderFiltered() {
        String q = searchField == null ? "" : safe(searchField.getText()).toLowerCase();
        if (q.isBlank()) {
            renderKanbanColumns(allTasks);
            return;
        }
        List<KanbanTask> filtered = new ArrayList<>();
        for (KanbanTask t : allTasks) {
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

        renderColumn(todoColumn, KanbanStatus.TODO, grouped.get(KanbanStatus.TODO));
        renderColumn(inProgressColumn, KanbanStatus.IN_PROGRESS, grouped.get(KanbanStatus.IN_PROGRESS));
        renderColumn(doneColumn, KanbanStatus.DONE, grouped.get(KanbanStatus.DONE));
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
                Tooltip tip = new Tooltip(desc);
                Tooltip.install(title, tip);
            }

            Region grow = new Region();
            HBox.setHgrow(grow, Priority.ALWAYS);

            Button left = new Button("<");
            left.getStyleClass().add("btn-ghost");
            left.setDisable(status == KanbanStatus.TODO);
            left.setOnAction(e -> moveTask(t, previous(status)));

            Button right = new Button(">");
            right.getStyleClass().add("btn-ghost");
            right.setDisable(status == KanbanStatus.DONE);
            right.setOnAction(e -> moveTask(t, next(status)));

            Button del = new Button("✖");
            del.getStyleClass().add("btn-danger");
            del.setOnAction(e -> deleteTask(t));

            HBox actions = new HBox(6, left, right, grow, del);
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
            reload(null);
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
        // style with rgb dialog look
        com.educompus.util.Dialogs.style(confirm);
        var res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }

        try {
            kanbanRepo.delete(task.getId());
            reload(null);
        } catch (Exception e) {
            error("Erreur", e);
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

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static void error(String title, Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(e == null ? "Erreur" : String.valueOf(e.getMessage()));
        com.educompus.util.Dialogs.style(a);
        a.showAndWait();
        if (e != null) {
            e.printStackTrace();
        }
    }
}

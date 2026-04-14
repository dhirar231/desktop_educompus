package com.educompus.controller.back;

import com.educompus.model.ExamAttemptRecord;
import com.educompus.repository.ExamRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;

public class TeacherResponsesController {

    @FXML private ChoiceBox<String> filterChoice;
    @FXML private ListView<ExamAttemptRecord> responsesList;

    private ObservableList<ExamAttemptRecord> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configure ListView cell rendering
        responsesList.setCellFactory(lv -> new ListCell<>() {
            private final VBox left = new VBox();
            private final HBox root = new HBox(12);
            private final javafx.scene.control.Label emailLbl = new javafx.scene.control.Label();
            private final javafx.scene.control.Label courseLbl = new javafx.scene.control.Label();
            private final javafx.scene.control.Label attemptsLbl = new javafx.scene.control.Label();
            private final javafx.scene.control.Label badge = new javafx.scene.control.Label();
            private final javafx.scene.control.Label certLbl = new javafx.scene.control.Label();
            {
                badge.getStyleClass().add("chip");
                left.getChildren().addAll(emailLbl, courseLbl);
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                root.getChildren().addAll(left, spacer, attemptsLbl, badge, certLbl);
            }

            @Override
            protected void updateItem(ExamAttemptRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                emailLbl.setText(item.getEmail());
                courseLbl.setText(item.getCourseName());
                attemptsLbl.setText(String.valueOf(item.getAttempts()));
                String passed = item.isPassed() ? "Oui" : "Non";
                badge.setText(passed);
                badge.getStyleClass().removeAll("chip-success", "chip-outline");
                badge.getStyleClass().add("Oui".equalsIgnoreCase(passed) ? "chip-success" : "chip-outline");
                certLbl.setText(item.getCertificatePath() == null ? "" : item.getCertificatePath());
                setGraphic(root);
                setText(null);
            }
        });

        filterChoice.getItems().addAll("Tous", "Certifiés", "Non certifiés");
        filterChoice.setValue("Tous");
        filterChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> applyFilter());

        loadData();
    }

    private void loadData() {
        try {
            ExamRepository repo = new ExamRepository();
            List<ExamAttemptRecord> list = repo.listAttemptsForExam(0); // examId 0 = all exams
            master.setAll(list);
            applyFilter();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private final ObservableList<ExamAttemptRecord> master = FXCollections.observableArrayList();

    private void applyFilter() {
        String sel = filterChoice.getValue();
        if (sel == null || sel.equals("Tous")) {
            data.setAll(master);
        } else if (sel.equals("Certifiés")) {
            data.setAll(master.filtered(ExamAttemptRecord::isPassed));
        } else if (sel.equals("Non certifiés")) {
            data.setAll(master.filtered(r -> !r.isPassed()));
        } else {
            data.setAll(master);
        }
        responsesList.setItems(data);
    }

    private void sortByAttemptsDesc() {
        FXCollections.sort(data, Comparator.comparingInt(ExamAttemptRecord::getAttempts).reversed());
    }
}

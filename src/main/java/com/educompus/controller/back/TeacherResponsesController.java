package com.educompus.controller.back;

import com.educompus.model.ExamAttemptRecord;
import com.educompus.repository.ExamRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Comparator;
import java.util.List;

public class TeacherResponsesController {

    @FXML private ChoiceBox<String> filterChoice;
    @FXML private TableView<ExamAttemptRecord> responsesTable;
    @FXML private TableColumn<ExamAttemptRecord, String> colEmail;
    @FXML private TableColumn<ExamAttemptRecord, String> colCourse;
    @FXML private TableColumn<ExamAttemptRecord, Integer> colAttempts;
    @FXML private TableColumn<ExamAttemptRecord, String> colPassed;
    @FXML private TableColumn<ExamAttemptRecord, String> colCertificate;

    private ObservableList<ExamAttemptRecord> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colCourse.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        colAttempts.setCellValueFactory(new PropertyValueFactory<>("attempts"));
        // show Oui/Non for passed
        colPassed.setCellValueFactory(cell -> {
            boolean passed = cell.getValue() != null && cell.getValue().isPassed();
            return new javafx.beans.property.SimpleStringProperty(passed ? "Oui" : "Non");
        });
        colCertificate.setCellValueFactory(new PropertyValueFactory<>("certificatePath"));

        // style passed cell as badge
        colPassed.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            private final javafx.scene.control.Label badge = new javafx.scene.control.Label();
            {
                badge.getStyleClass().add("chip");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
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
        responsesTable.setItems(data);
    }

    private void sortByAttemptsDesc() {
        FXCollections.sort(data, Comparator.comparingInt(ExamAttemptRecord::getAttempts).reversed());
    }
}

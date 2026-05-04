package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.model.Cours;
import com.educompus.repository.CoursValidationRepository;
import com.educompus.service.CoursWorkflowService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

public final class BackValidationController {

    private final CoursValidationRepository validationRepo = new CoursValidationRepository();
    private final CoursWorkflowService workflowService = new CoursWorkflowService();

    @FXML private ListView<Cours> enAttenteListView;
    @FXML private ListView<Cours> approuveListView;
    @FXML private ListView<Cours> refuseListView;
    @FXML private Label compteurEnAttenteLabel;
    @FXML private VBox emptyStatePane;

    @FXML
    private void initialize() {
        setupCellFactories();
        reloadLists();
    }

    private void setupCellFactories() {
        if (enAttenteListView != null) enAttenteListView.setCellFactory(lv -> new CoursCell());
        if (approuveListView != null) approuveListView.setCellFactory(lv -> new CoursCell());
        if (refuseListView != null) refuseListView.setCellFactory(lv -> new CoursCell());
    }

    @FXML
    private void onApprouver() {
        if (enAttenteListView == null) return;
        Cours selected = enAttenteListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Aucune sélection", "Veuillez sélectionner un cours à approuver.");
            return;
        }
        try {
            workflowService.approuver(selected.getId(), AppState.getUserId());
            showAlert(Alert.AlertType.INFORMATION, "Cours approuvé",
                    "Le cours « " + selected.getTitre() + " » a été approuvé.");
            reloadLists();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void onRefuser() {
        if (enAttenteListView == null) return;
        Cours selected = enAttenteListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Aucune sélection", "Veuillez sélectionner un cours à refuser.");
            return;
        }
        Optional<String> commentaire = showCommentaireDialog(selected);
        if (commentaire.isEmpty()) return;
        try {
            workflowService.refuser(selected.getId(), AppState.getUserId(), commentaire.get());
            showAlert(Alert.AlertType.INFORMATION, "Cours refusé",
                    "Le cours « " + selected.getTitre() + " » a été refusé.");
            reloadLists();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        reloadLists();
    }

    void reloadLists() {
        List<Cours> enAttente = validationRepo.listCoursEnAttente();
        List<Cours> approuves = validationRepo.listCoursApprouves("");
        List<Cours> refusesList = validationRepo.listCoursRefuses();

        if (enAttenteListView != null)
            enAttenteListView.setItems(FXCollections.observableArrayList(enAttente));
        if (approuveListView != null)
            approuveListView.setItems(FXCollections.observableArrayList(approuves));
        if (refuseListView != null)
            refuseListView.setItems(FXCollections.observableArrayList(refusesList));

        int count = enAttente.size();
        if (compteurEnAttenteLabel != null)
            compteurEnAttenteLabel.setText(count + " cours en attente");

        boolean empty = enAttente.isEmpty() && approuves.isEmpty() && refusesList.isEmpty();
        if (emptyStatePane != null) {
            emptyStatePane.setVisible(empty);
            emptyStatePane.setManaged(empty);
        }
    }

    private Optional<String> showCommentaireDialog(Cours cours) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Refuser le cours");
        dialog.setHeaderText("Refuser : « " + cours.getTitre() + " »");
        dialog.setContentText("Commentaire de refus :");
        return dialog.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class CoursCell extends ListCell<Cours> {
        private final Label titleLbl = new Label();
        private final Label metaLbl = new Label();
        private final Label badgeLbl = new Label();
        private final HBox row;

        CoursCell() {
            titleLbl.getStyleClass().add("project-card-title");
            metaLbl.getStyleClass().add("page-subtitle");
            metaLbl.setStyle("-fx-font-size: 11px;");
            VBox info = new VBox(2, titleLbl, metaLbl);
            HBox.setHgrow(info, Priority.ALWAYS);
            row = new HBox(10, info, badgeLbl);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
        }

        @Override
        protected void updateItem(Cours c, boolean empty) {
            super.updateItem(c, empty);
            if (empty || c == null) { setGraphic(null); return; }
            titleLbl.setText(c.getTitre() == null ? "" : c.getTitre());
            metaLbl.setText((c.getDomaine() == null ? "" : c.getDomaine())
                    + "  •  " + (c.getNomFormateur() == null ? "" : c.getNomFormateur()));
            if (c.getStatut() != null) {
                badgeLbl.setText(c.getStatut().libelle());
                badgeLbl.getStyleClass().setAll(c.getStatut().badgeCssClass());
            }
            setGraphic(row);
        }
    }
}


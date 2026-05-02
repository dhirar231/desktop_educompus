package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.model.Cours;
import com.educompus.model.CoursStatut;
import com.educompus.repository.CoursValidationRepository;
import com.educompus.service.CoursWorkflowService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public final class TeacherCoursesController {

    private final CoursValidationRepository validationRepo = new CoursValidationRepository();
    private final CoursWorkflowService workflowService = new CoursWorkflowService();

    @FXML private ListView<Cours> coursListView;
    @FXML private Button addCoursBtn;

    @FXML
    private void initialize() {
        if (coursListView != null) {
            coursListView.setCellFactory(lv -> new CoursCell());
        }
        reloadCours();
    }

    @FXML
    private void onAddCours() {
        showAlert(Alert.AlertType.INFORMATION, "Ajouter un cours",
                "Utilisez le formulaire de création de cours dans la section Cours.");
    }

    @FXML
    private void onEditCours() {
        if (coursListView == null) return;
        Cours selected = coursListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Aucune sélection", "Veuillez sélectionner un cours.");
            return;
        }
        if (selected.getStatut() == CoursStatut.REFUSE) {
            try {
                workflowService.reinitialiserPourModification(selected.getId());
                showAlert(Alert.AlertType.INFORMATION, "Cours remis en attente",
                        "Le cours « " + selected.getTitre() + " » a été remis en attente de validation.");
                reloadCours();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Modification non disponible",
                    "Seuls les cours refusés peuvent être soumis à nouveau. Statut actuel : "
                            + selected.getStatut().libelle());
        }
    }

    void reloadCours() {
        List<Cours> cours = validationRepo.listCoursByEnseignant(AppState.getUserId());
        if (coursListView != null) {
            coursListView.setItems(FXCollections.observableArrayList(cours));
        }
    }

    Label buildBadge(CoursStatut statut) {
        Label badge = new Label(statut.libelle());
        badge.getStyleClass().add(statut.badgeCssClass());
        return badge;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private class CoursCell extends ListCell<Cours> {
        private final Label titleLbl = new Label();
        private final Label metaLbl = new Label();
        private final Label commentaireLbl = new Label();
        private final Label badgeLbl = new Label();
        private final HBox row;
        private final VBox info;

        CoursCell() {
            titleLbl.getStyleClass().add("project-card-title");
            metaLbl.getStyleClass().add("page-subtitle");
            metaLbl.setStyle("-fx-font-size: 11px;");
            commentaireLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-wrap-text: true;");
            info = new VBox(2, titleLbl, metaLbl, commentaireLbl);
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
                    + "  •  " + c.getDureeTotaleHeures() + "h");

            boolean hasComment = c.getStatut() == CoursStatut.REFUSE
                    && c.getCommentaireAdmin() != null
                    && !c.getCommentaireAdmin().isBlank();
            commentaireLbl.setText(hasComment ? "💬 " + c.getCommentaireAdmin() : "");
            commentaireLbl.setVisible(hasComment);
            commentaireLbl.setManaged(hasComment);

            if (c.getStatut() != null) {
                badgeLbl.setText(c.getStatut().libelle());
                badgeLbl.getStyleClass().setAll(c.getStatut().badgeCssClass());
            }
            setGraphic(row);
        }
    }
}

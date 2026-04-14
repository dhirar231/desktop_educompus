package com.educompus.controller.back;

import com.educompus.model.AuthUser;
import com.educompus.service.AuthUserService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BackUserImportController {

    @FXML
    private Label subtitleLabel;
    @FXML
    private Label statLabel;
    @FXML
    private ComboBox<String> duplicatePolicyCombo;
    @FXML
    private TableView<ImportItem> importTable;
    @FXML
    private TableColumn<ImportItem, Boolean> colSelect;
    @FXML
    private TableColumn<ImportItem, String> colStatus;
    @FXML
    private TableColumn<ImportItem, String> colDisplayName;
    @FXML
    private TableColumn<ImportItem, String> colEmail;
    @FXML
    private TableColumn<ImportItem, String> colRole;

    private final AuthUserService service = new AuthUserService();
    private final ObservableList<ImportItem> items = FXCollections.observableArrayList();
    private Runnable onSuccess;

    @FXML
    private void initialize() {
        duplicatePolicyCombo.getItems().setAll("Ignorer les doublons", "Remplacer les données");
        duplicatePolicyCombo.getSelectionModel().selectFirst();

        colSelect.setCellValueFactory(cb -> cb.getValue().selectedProperty());
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setEditable(true);
        importTable.setEditable(true);

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();
            {
                label.getStyleClass().add("chip");
                label.setPadding(new javafx.geometry.Insets(2, 8, 2, 8));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    label.getStyleClass().removeAll("chip-success", "chip-warning");
                    if (item.equalsIgnoreCase("Nouveau")) {
                        label.getStyleClass().add("chip-success");
                    } else {
                        label.getStyleClass().add("chip-warning");
                    }
                    setGraphic(label);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        colDisplayName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUser().displayName()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUser().email()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(getRoleLabel(c.getValue().getUser())));

        importTable.setItems(items);
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    public void loadFile(File file) {
        if (file == null || !file.exists())
            return;
        items.clear();
        int count = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    // Check for Byte Order Mark (BOM) and remove it if present
                    if (line.startsWith("\ufeff")) {
                        line = line.substring(1);
                    }
                    // Skip header if it contains expected keywords
                    if (line.toLowerCase().contains("email") || line.toLowerCase().contains("nom")) {
                        firstLine = false;
                        continue;
                    }
                }

                String[] parts = line.split(";");
                if (parts.length < 3)
                    continue;

                // Format: ID;Nom;Email;password;Rôle;Admin;Enseignant
                // But we mainly need Nom, Email, Password, Admin, Enseignant
                String name = parts.length > 1 ? parts[1].trim() : "";
                String email = parts.length > 2 ? parts[2].trim() : "";
                String password = parts.length > 3 ? parts[3].trim() : "EduCompus2026!";
                boolean admin = parts.length > 5 && parts[5].trim().equalsIgnoreCase("Oui");
                boolean teacher = parts.length > 6 && parts[6].trim().equalsIgnoreCase("Oui");

                if (email.isEmpty())
                    continue;

                AuthUser existing = service.findByEmail(email);
                AuthUser user = new AuthUser(existing != null ? existing.id() : 0, email, name, "", admin, teacher);

                ImportItem item = new ImportItem(user, existing != null ? "Doublon" : "Nouveau", password);
                items.add(item);
                count++;
            }
            statLabel.setText(count + " utilisateur(s) détecté(s)");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de lire le fichier CSV : " + e.getMessage());
        }
    }

    @FXML
    private void onSelectAll() {
        items.forEach(i -> i.setSelected(true));
    }

    @FXML
    private void onDeselectAll() {
        items.forEach(i -> i.setSelected(false));
    }

    @FXML
    private void onConfirm() {
        long selectedCount = items.stream().filter(ImportItem::isSelected).count();
        if (selectedCount == 0) {
            showAlert(Alert.AlertType.WARNING, "Importation", "Aucun utilisateur sélectionné.");
            return;
        }

        boolean replaceDuplicates = "Remplacer les données".equals(duplicatePolicyCombo.getValue());
        int imported = 0;
        int skipped = 0;
        int errors = 0;

        for (ImportItem item : items) {
            if (!item.isSelected())
                continue;

            try {
                if (item.getStatus().equals("Nouveau")) {
                    service.create(item.getUser(), item.getPassword());
                    imported++;
                } else if (replaceDuplicates) {
                    service.update(item.getUser(), item.getPassword());
                    imported++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                errors++;
                e.printStackTrace();
            }
        }

        String msg = imported + " users importés/mis à jour.";
        if (skipped > 0)
            msg += "\n" + skipped + " doublons ignorés.";
        if (errors > 0)
            msg += "\n" + errors + " erreurs rencontrées.";

        showAlert(Alert.AlertType.INFORMATION, "Importation terminée", msg);

        if (onSuccess != null)
            onSuccess.run();
        onCancel();
    }

    @FXML
    private void onCancel() {
        ((Stage) subtitleLabel.getScene().getWindow()).close();
    }

    private String getRoleLabel(AuthUser user) {
        if (user.admin() && user.teacher())
            return "Admin + Enseignant";
        if (user.admin())
            return "Administrateur";
        if (user.teacher())
            return "Enseignant";
        return "Utilisateur";
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        if (subtitleLabel.getScene() != null) {
            alert.getDialogPane().getStylesheets().addAll(subtitleLabel.getScene().getStylesheets());
        }
        alert.showAndWait();
    }

    public static class ImportItem {
        private final BooleanProperty selected = new SimpleBooleanProperty(true);
        private final AuthUser user;
        private final String status;
        private final String password;

        public ImportItem(AuthUser user, String status, String password) {
            this.user = user;
            this.status = status;
            this.password = password;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean val) {
            selected.set(val);
        }

        public BooleanProperty selectedProperty() {
            return selected;
        }

        public AuthUser getUser() {
            return user;
        }

        public String getStatus() {
            return status;
        }

        public String getPassword() {
            return password;
        }
    }
}

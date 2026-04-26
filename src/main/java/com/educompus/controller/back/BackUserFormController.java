package com.educompus.controller.back;

import com.educompus.model.AuthUser;
import com.educompus.service.AuthUserService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class BackUserFormController {

    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;
    @FXML
    private Label passwordHint;
    @FXML
    private Label errorLabel;

    @FXML
    private TextField displayNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField imageField;
    @FXML
    private ComboBox<String> roleField;
    @FXML
    private PasswordField passwordField;

    private final AuthUserService service = new AuthUserService();
    private Runnable onSuccess;
    private AuthUser editingUser;
    private boolean createMode;

    @FXML
    private void initialize() {
        roleField.getItems().setAll(
                "Utilisateur",
                "Enseignant",
                "Administrateur",
                "Administrateur + Enseignant"
        );
        roleField.getSelectionModel().select("Utilisateur");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    public void prepareCreate() {
        createMode = true;
        editingUser = null;
        titleLabel.setText("Ajouter un utilisateur");
        subtitleLabel.setText("Creer un compte et attribuer son role.");
        passwordHint.setText("Mot de passe requis (min 6 caracteres).");
        passwordField.clear();
    }

    public void prepareUpdate(AuthUser user) {
        createMode = false;
        editingUser = user;
        titleLabel.setText("Modifier un utilisateur");
        subtitleLabel.setText("Mettre a jour les informations et le role.");
        passwordHint.setText("Laisser vide pour conserver le mot de passe actuel.");

        displayNameField.setText(user.displayName());
        emailField.setText(user.email());
        imageField.setText(user.imageUrl() == null ? "" : user.imageUrl());
        roleField.setValue(roleFromUser(user));
        passwordField.clear();
    }

    @FXML
    private void onSave() {
        hideError();
        try {
            AuthUser payload = toPayload();
            if (createMode) {
                service.create(payload, passwordField.getText());
            } else {
                service.update(payload, passwordField.getText());
            }

            if (onSuccess != null) {
                onSuccess.run();
            }
            close();
        } catch (Exception e) {
            showError(e.getMessage() == null ? "Operation impossible." : e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private AuthUser toPayload() {
        String role = roleField.getValue() == null ? "Utilisateur" : roleField.getValue();
        boolean admin = role.contains("Administrateur");
        boolean teacher = role.contains("Enseignant");

        int id = createMode ? 0 : editingUser.id();
        return new AuthUser(
                id,
                safe(emailField.getText()).toLowerCase(),
                safe(displayNameField.getText()),
                safe(imageField.getText()),
                admin,
                teacher
        );
    }

    private String roleFromUser(AuthUser user) {
        if (user.admin() && user.teacher()) {
            return "Administrateur + Enseignant";
        }
        if (user.admin()) {
            return "Administrateur";
        }
        if (user.teacher()) {
            return "Enseignant";
        }
        return "Utilisateur";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void showError(String message) {
        errorLabel.setText(message == null ? "Erreur." : message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void close() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }
}

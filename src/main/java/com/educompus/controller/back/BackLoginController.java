package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.nav.Navigator;
import com.educompus.service.DbAuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public final class BackLoginController {
    @FXML
    private TextField email;

    @FXML
    private PasswordField password;

    @FXML
    private void signIn(ActionEvent event) {
        String mail = email == null ? "" : String.valueOf(email.getText()).trim().toLowerCase();
        String pass = password == null ? "" : String.valueOf(password.getText()).trim();
        if (mail.isBlank() || pass.isBlank()) {
            Navigator.goRoot("View/front/FrontLogin.fxml");
            return;
        }

        try {
            var user = DbAuthService.authenticate(mail, pass);
            if (user == null || !user.admin()) {
                Navigator.goRoot("View/front/FrontLogin.fxml");
                return;
            }
            AppState.setRole(AppState.Role.ADMIN);
            AppState.setUserEmail(user.email());
            AppState.setUserDisplayName(user.displayName());
            AppState.setUserImageUrl(user.imageUrl());
            Navigator.goRoot("View/back/BackShell.fxml");
        } catch (Exception e) {
            Navigator.goRoot("View/front/FrontLogin.fxml");
        }
    }
}

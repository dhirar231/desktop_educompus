package com.educompus.controller.front;

import com.educompus.app.AppState;
import com.educompus.model.AuthUser;
import com.educompus.nav.Navigator;
import com.educompus.service.DbAuthService;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.prefs.Preferences;

public final class FrontLoginController {
    private static final String PREF_REMEMBER = "rememberMe";
    private static final String PREF_EMAIL = "rememberEmail";

    @FXML
    private TextField email;

    @FXML
    private PasswordField password;

    @FXML
    private VBox sidePane;

    @FXML
    private StackPane cardPane;

    @FXML
    private VBox loginPane;

    @FXML
    private VBox signUpPane;

    @FXML
    private HBox authContainer;

    @FXML
    private StackPane authShell;

    @FXML
    private HBox tabsRow;

    @FXML
    private ToggleButton tabLogin;

    @FXML
    private ToggleButton tabSignup;

    @FXML
    private CheckBox rememberMe;

    @FXML
    private Label errorLabel;

    @FXML
    private ImageView logoImage;

    @FXML
    private Label logoFallback;

    @FXML
    private TextField signupName;

    @FXML
    private TextField signupEmail;

    @FXML
    private PasswordField signupPassword;

    @FXML
    private PasswordField signupPassword2;

    @FXML
    private Label signupInfoLabel;

    @FXML
    private VBox forgotPane;

    @FXML
    private TextField forgotEmail;

    @FXML
    private Label forgotInfoLabel;

    private Animation errorAutoHide;
    private Timeline rgbBorderTimeline;
    private boolean swapped = false;
    private Preferences prefs;

    @FXML
    private void initialize() {
        if (tabLogin != null) {
            tabLogin.setSelected(true);
        }

        loadLogoFromResources();
        initRememberMe();
        Platform.runLater(() -> {
            playIntroAnimation();
            startRgbBorderAnimation();
        });
    }

    private void initRememberMe() {
        try {
            prefs = Preferences.userNodeForPackage(FrontLoginController.class);
        } catch (Exception ignored) {
            prefs = null;
        }

        if (prefs == null) {
            return;
        }

        boolean remember = false;
        String rememberedEmail = "";
        try {
            remember = prefs.getBoolean(PREF_REMEMBER, false);
            rememberedEmail = prefs.get(PREF_EMAIL, "");
        } catch (Exception ignored) {
        }

        if (rememberMe != null) {
            rememberMe.setSelected(remember);
            rememberMe.selectedProperty().addListener((obs, oldV, newV) -> {
                if (prefs == null) {
                    return;
                }
                try {
                    prefs.putBoolean(PREF_REMEMBER, Boolean.TRUE.equals(newV));
                    if (!Boolean.TRUE.equals(newV)) {
                        prefs.remove(PREF_EMAIL);
                    } else if (email != null) {
                        String mail = String.valueOf(email.getText()).trim().toLowerCase();
                        if (!mail.isBlank()) {
                            prefs.put(PREF_EMAIL, mail);
                        }
                    }
                } catch (Exception ignored) {
                }
            });
        }

        if (email != null) {
            if (remember && !rememberedEmail.isBlank()) {
                email.setText(rememberedEmail);
            }
            email.textProperty().addListener((obs, oldV, newV) -> {
                if (prefs == null || rememberMe == null || !rememberMe.isSelected()) {
                    return;
                }
                try {
                    String mail = newV == null ? "" : newV.trim().toLowerCase();
                    if (mail.isBlank()) {
                        prefs.remove(PREF_EMAIL);
                    } else {
                        prefs.put(PREF_EMAIL, mail);
                    }
                } catch (Exception ignored) {
                }
            });
        }
    }

    private void loadLogoFromResources() {
        if (logoImage == null) {
            return;
        }

        try {
            var url = FrontLoginController.class.getResource("/assets/images/logo-light.png");
            if (url == null) {
                return;
            }

            logoImage.setImage(new javafx.scene.image.Image(url.toExternalForm(), true));
            logoImage.setOpacity(1.0);
            if (logoFallback != null) {
                logoFallback.setVisible(false);
                logoFallback.setManaged(false);
            }
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void signIn(ActionEvent event) {
        String mail = email == null ? "" : String.valueOf(email.getText()).trim().toLowerCase();
        String pass = password == null ? "" : String.valueOf(password.getText()).trim();
        if (mail.isBlank() || pass.isBlank()) {
            showError("Veuillez saisir votre email et votre mot de passe.");
            shake(cardPane);
            return;
        }

        var user = authenticateOrShowError(mail, pass);
        if (user == null) {
            shake(cardPane);
            return;
        }

        AppState.setRole(user.admin() ? AppState.Role.ADMIN : AppState.Role.USER);
        AppState.setUserEmail(user.email());
        AppState.setUserDisplayName(user.displayName());
        AppState.setUserImageUrl(user.imageUrl());

        persistRememberMe(mail);

        try {
            Navigator.goRoot(user.admin() ? "View/back/BackShell.fxml" : "View/front/FrontShell.fxml");
        } catch (Exception e) {
            showError("Erreur interface: " + summarizeThrowable(e));
            e.printStackTrace();
            shake(cardPane);
        }
    }

    private void persistRememberMe(String mail) {
        if (prefs == null) {
            return;
        }
        boolean remember = rememberMe != null && rememberMe.isSelected();
        try {
            prefs.putBoolean(PREF_REMEMBER, remember);
            if (remember) {
                prefs.put(PREF_EMAIL, mail == null ? "" : mail.trim().toLowerCase());
            } else {
                prefs.remove(PREF_EMAIL);
            }
        } catch (Exception ignored) {
        }
    }

    private AuthUser authenticateOrShowError(String mail, String pass) {
        try {
            var user = DbAuthService.authenticate(mail, pass);
            if (user == null) {
                showError("Email ou mot de passe incorrect.");
                return null;
            }
            return user;
        } catch (Exception e) {
            showError("Connexion DB impossible: " + summarizeThrowable(e));
            e.printStackTrace();
            return null;
        }
    }

    private String summarizeThrowable(Throwable t) {
        if (t == null) {
            return "unknown error";
        }
        if (t instanceof IllegalStateException && t.getMessage() != null && t.getMessage().contains("Failed to load FXML:")) {
            String msg = t.getMessage().replace('\n', ' ').replace('\r', ' ').trim();
            if (msg.length() > 180) {
                msg = msg.substring(0, 180) + "...";
            }
            return msg;
        }
        Throwable cur = t;
        int hops = 0;
        while (cur.getCause() != null && cur.getCause() != cur && hops < 6) {
            cur = cur.getCause();
            hops++;
        }

        String msg = cur.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = t.getMessage();
        }
        msg = msg == null ? "" : msg.replace('\n', ' ').replace('\r', ' ').trim();
        if (msg.length() > 160) {
            msg = msg.substring(0, 160) + "...";
        }

        String name = cur.getClass().getSimpleName();
        return msg.isBlank() ? name : (name + ": " + msg);
    }

    @FXML
    private void showLogin(ActionEvent event) {
        if (tabLogin != null) {
            tabLogin.setSelected(true);
        }
        swapPanels(false);
        showTabs(true);
        showForgotPane(false);
        switchForm(loginPane, signUpPane, -1);
    }

    @FXML
    private void showSignUp(ActionEvent event) {
        if (tabSignup != null) {
            tabSignup.setSelected(true);
        }
        swapPanels(true);
        showTabs(true);
        showForgotPane(false);
        switchForm(signUpPane, loginPane, 1);
    }

    @FXML
    private void forgotPassword(ActionEvent event) {
        String mail = email == null ? "" : String.valueOf(email.getText()).trim().toLowerCase();
        if (forgotEmail != null) {
            forgotEmail.setText(mail);
        }
        hideError();
        showTabs(false);
        showForgotPane(true);
    }

    @FXML
    private void backFromForgot(ActionEvent event) {
        showTabs(true);
        showForgotPane(false);
        showLogin(event);
    }

    @FXML
    private void sendResetLink(ActionEvent event) {
        String mail = forgotEmail == null ? "" : String.valueOf(forgotEmail.getText()).trim().toLowerCase();
        if (mail.isBlank()) {
            if (forgotInfoLabel != null) {
                forgotInfoLabel.setText("Veuillez saisir votre email.");
                forgotInfoLabel.setManaged(true);
                forgotInfoLabel.setVisible(true);
            }
            return;
        }

        try {
            boolean exists = DbAuthService.emailExists(mail);
            if (forgotInfoLabel != null) {
                forgotInfoLabel.setText(exists
                        ? "Si ce compte existe, un lien de réinitialisation sera envoyé (à brancher sur Symfony)."
                        : "Si ce compte existe, un lien de réinitialisation sera envoyé (à brancher sur Symfony).");
                forgotInfoLabel.setManaged(true);
                forgotInfoLabel.setVisible(true);
            }
        } catch (Exception e) {
            if (forgotInfoLabel != null) {
                String msg = String.valueOf(e.getMessage());
                if (msg.length() > 120) {
                    msg = msg.substring(0, 120) + "...";
                }
                forgotInfoLabel.setText("DB: " + msg);
                forgotInfoLabel.setManaged(true);
                forgotInfoLabel.setVisible(true);
            }
            e.printStackTrace();
        }
    }

    @FXML
    private void signUp(ActionEvent event) {
        String p1 = signupPassword == null ? "" : String.valueOf(signupPassword.getText());
        String p2 = signupPassword2 == null ? "" : String.valueOf(signupPassword2.getText());
        if (!p1.equals(p2)) {
            if (signupInfoLabel != null) {
                signupInfoLabel.setText("Les mots de passe ne correspondent pas.");
            }
            shake(cardPane);
            return;
        }
        if (signupInfoLabel != null) {
            signupInfoLabel.setText("Compte créé (template). Vous pouvez vous connecter.");
        }
        showLogin(event);
    }

    private void playIntroAnimation() {
        setSwapClasses(false);
        animateIn(sidePane, -16);
        animateIn(cardPane, 16);
    }

    private void showTabs(boolean show) {
        if (tabsRow == null) {
            return;
        }
        tabsRow.setVisible(show);
        tabsRow.setManaged(show);
    }

    private void showForgotPane(boolean show) {
        if (forgotPane == null) {
            return;
        }
        forgotPane.setVisible(show);
        forgotPane.setManaged(show);
        if (show) {
            if (loginPane != null) {
                loginPane.setVisible(false);
                loginPane.setManaged(false);
            }
            if (signUpPane != null) {
                signUpPane.setVisible(false);
                signUpPane.setManaged(false);
            }
        } else {
            if (forgotInfoLabel != null) {
                forgotInfoLabel.setVisible(false);
                forgotInfoLabel.setManaged(false);
            }
        }
    }

    private void animateIn(Node node, double fromX) {
        if (node == null) {
            return;
        }
        node.setOpacity(0.0);
        node.setTranslateX(fromX);

        FadeTransition fade = new FadeTransition(Duration.millis(420), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(420), node);
        slide.setFromX(fromX);
        slide.setToX(0.0);

        ParallelTransition intro = new ParallelTransition(fade, slide);
        intro.play();
    }

    private void switchForm(VBox toShow, VBox toHide, int direction) {
        if (toShow == null || toHide == null || toShow.isVisible()) {
            return;
        }

        hideError();
        double offset = 18.0;

        toShow.setManaged(true);
        toShow.setVisible(true);
        toShow.setOpacity(0.0);
        toShow.setTranslateX(direction * offset);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), toShow);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(220), toShow);
        slideIn.setFromX(direction * offset);
        slideIn.setToX(0.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), toHide);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(180), toHide);
        slideOut.setFromX(0.0);
        slideOut.setToX(-direction * offset);

        ParallelTransition transition = new ParallelTransition(fadeIn, slideIn, fadeOut, slideOut);
        transition.setOnFinished(e -> {
            toHide.setVisible(false);
            toHide.setManaged(false);
            toHide.setOpacity(1.0);
            toHide.setTranslateX(0.0);
        });
        transition.play();
    }

    private void showError(String message) {
        if (errorLabel == null) {
            return;
        }

        if (errorAutoHide != null) {
            errorAutoHide.stop();
        }

        errorLabel.setText(message == null ? "" : message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
        errorLabel.setOpacity(0.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(160), errorLabel);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> hideError());
        pause.play();
        errorAutoHide = pause;
    }

    private void hideError() {
        if (errorLabel == null || !errorLabel.isVisible()) {
            return;
        }
        FadeTransition fadeOut = new FadeTransition(Duration.millis(140), errorLabel);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            errorLabel.setOpacity(1.0);
        });
        fadeOut.play();
    }

    private void shake(Node node) {
        if (node == null) {
            return;
        }
        TranslateTransition shake = new TranslateTransition(Duration.millis(45), node);
        shake.setByX(8);
        shake.setAutoReverse(true);
        shake.setCycleCount(6);
        shake.play();
    }

    private void startRgbBorderAnimation() {
        if (authShell == null) {
            return;
        }
        if (rgbBorderTimeline != null) {
            rgbBorderTimeline.stop();
        }

        rgbBorderTimeline = new Timeline(new KeyFrame(Duration.millis(80), e -> {
            double t = (System.currentTimeMillis() % 8000) / 8000.0;

            double h1 = 195 + (Math.sin(t * Math.PI * 2) * 12);
            double h2 = 215 + (Math.sin((t + 0.33) * Math.PI * 2) * 14);
            double h3 = 245 + (Math.sin((t + 0.66) * Math.PI * 2) * 12);

            String c1 = hsbCss(h1, 0.95, 1.0, 0.92);
            String c2 = hsbCss(h2, 0.92, 1.0, 0.92);
            String c3 = hsbCss(h3, 0.92, 1.0, 0.92);

            String border = "linear-gradient(to right, " + c1 + ", " + c2 + ", " + c3 + ")";
            authShell.setStyle("-fx-border-color: " + border + ";");
        }));

        rgbBorderTimeline.setCycleCount(Animation.INDEFINITE);
        rgbBorderTimeline.play();
    }

    private String hsbCss(double hue, double saturation, double brightness, double alpha) {
        javafx.scene.paint.Color c = javafx.scene.paint.Color.hsb(hue, saturation, brightness, alpha);
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        return String.format("rgba(%d,%d,%d,%.3f)", r, g, b, alpha);
    }

    private void setSwapClasses(boolean signUpSelected) {
        swapped = signUpSelected;
        if (sidePane != null) {
            sidePane.getStyleClass().removeAll("left", "right");
            sidePane.getStyleClass().add(swapped ? "right" : "left");
        }
        if (cardPane != null) {
            cardPane.getStyleClass().removeAll("left", "right");
            cardPane.getStyleClass().add(swapped ? "left" : "right");
        }
    }

    private void swapPanels(boolean signUpSelected) {
        if (authContainer == null || sidePane == null || cardPane == null) {
            setSwapClasses(signUpSelected);
            return;
        }
        if (swapped == signUpSelected) {
            return;
        }

        double sideW = sidePane.getBoundsInParent().getWidth();
        double cardW = cardPane.getBoundsInParent().getWidth();
        if (sideW <= 0 || cardW <= 0) {
            Platform.runLater(() -> swapPanels(signUpSelected));
            return;
        }

        double sideDelta = signUpSelected ? cardW : -cardW;
        double cardDelta = signUpSelected ? -sideW : sideW;

        TranslateTransition t1 = new TranslateTransition(Duration.millis(320), sidePane);
        t1.setByX(sideDelta);

        TranslateTransition t2 = new TranslateTransition(Duration.millis(320), cardPane);
        t2.setByX(cardDelta);

        ParallelTransition pt = new ParallelTransition(t1, t2);
        pt.setOnFinished(e -> {
            setSwapClasses(signUpSelected);
            if (signUpSelected) {
                authContainer.getChildren().setAll(cardPane, sidePane);
            } else {
                authContainer.getChildren().setAll(sidePane, cardPane);
            }
            sidePane.setTranslateX(0);
            cardPane.setTranslateX(0);
        });
        pt.play();
    }
}

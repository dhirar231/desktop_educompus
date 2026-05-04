package com.educompus.controller.front;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.educompus.app.AppState;
import com.educompus.model.AuthUser;
import com.educompus.nav.Navigator;
import com.educompus.service.DbAuthService;
import com.educompus.service.SavedAccountService;
import com.educompus.service.WindowsHelloAuthService;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import com.educompus.model.PlacementQuestion;
import com.educompus.repository.UserRepository;
import com.educompus.service.PlacementGenerator;
import com.educompus.ui.PlacementDialog;

public final class FrontLoginController {
    private static final String PREF_REMEMBER = "rememberMe";
    private static final String PREF_EMAIL = "rememberEmail";
    private static final double CAPTCHA_MIN_HEIGHT = 234.0;
    private static final double CAPTCHA_MAX_HEIGHT = 640.0;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    private static final Pattern CAPTCHA_SUCCESS_PATTERN = Pattern.compile("\\\"success\\\"\\s*:\\s*true");
    private static final Object CAPTCHA_SERVER_LOCK = new Object();
    private static volatile Map<String, String> dotEnvValues;
    private static volatile HttpServer captchaLocalServer;
    private static volatile String captchaLocalServerUrl;
    private static volatile String captchaSiteKey;

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
    private VBox quickLoginPane;

    @FXML
    private VBox savedAccountsList;

    @FXML
    private VBox manualLoginPane;

    @FXML
    private HBox savedAccountsSwitchRow;

    @FXML
    private Label errorLabel;

    @FXML
    private WebView captchaView;

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
    private String captchaToken = "";
    private String recaptchaSecretKey = "";
    private final SavedAccountService savedAccountService = new SavedAccountService();

    @FXML
    private void initialize() {
        if (tabLogin != null) {
            tabLogin.setSelected(true);
        }

        loadLogoFromResources();
        initRememberMe();
        refreshSavedAccountsUi();
        initCaptcha();
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

        if (!verifyCaptchaOrShowError()) {
            shake(cardPane);
            return;
        }

        var user = authenticateOrShowError(mail, pass);
        if (user == null) {
            shake(cardPane);
            return;
        }

        maybeOfferCredentialSave(mail, pass);
        persistRememberMe(mail);
        completeLogin(user);
    }

    private void completeLogin(AuthUser user) {
        if (user == null) {
            return;
        }

        if (user.admin()) {
            AppState.setRole(AppState.Role.ADMIN);
        } else if (user.teacher()) {
            AppState.setRole(AppState.Role.TEACHER);
        } else {
            AppState.setRole(AppState.Role.USER);
        }
        AppState.setUserId(user.id());
        AppState.setUserEmail(user.email());
        AppState.setUserDisplayName(user.displayName());
        AppState.setUserImageUrl(user.imageUrl());

        try {
            String target = (user.admin() || user.teacher()) ? "View/back/BackShell.fxml"
                    : "View/front/FrontShell.fxml";
            Navigator.goRoot(target);
            // After navigation, show placement only for regular student users (once)
            try {
                if (!AppState.isAdmin() && !AppState.isTeacher()) {
                    UserRepository ur = new UserRepository();
                    if (!ur.hasCompletedPlacement(user.email())) {
                        var qs = PlacementGenerator.generate(10, 10);
                        javafx.stage.Window owner = Navigator.getStage();
                        if (owner == null && cardPane != null && cardPane.getScene() != null)
                            owner = cardPane.getScene().getWindow();
                        PlacementDialog dlg = new PlacementDialog(owner, qs, user.email());
                        boolean done = dlg.showAndWait();
                        if (!done) {
                            // If user explicitly abandoned the placement dialog, exit the application
                            if (dlg.wasAbandoned()) {
                                Platform.exit();
                                return;
                            }
                            showError("Veuillez compléter le test de placement pour continuer.");
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            showError("Erreur interface: " + summarizeThrowable(e));
            e.printStackTrace();
            shake(cardPane);
        }
    }

    private void refreshSavedAccountsUi() {
        List<SavedAccountService.SavedAccountEntry> accounts = savedAccountService.listAccounts();
        boolean hasSavedAccounts = !accounts.isEmpty();

        if (savedAccountsList != null) {
            savedAccountsList.getChildren().clear();
            for (SavedAccountService.SavedAccountEntry account : accounts) {
                savedAccountsList.getChildren().add(buildSavedAccountCard(account));
            }
        }

        if (savedAccountsSwitchRow != null) {
            savedAccountsSwitchRow.setManaged(hasSavedAccounts);
            savedAccountsSwitchRow.setVisible(hasSavedAccounts);
        }

        if (hasSavedAccounts) {
            setManualLoginVisible(false);
        } else {
            setManualLoginVisible(true);
        }
    }

    private Node buildSavedAccountCard(SavedAccountService.SavedAccountEntry account) {
        HBox card = new HBox(10);
        card.getStyleClass().add("saved-account-card");

        Label avatar = new Label(extractAvatarLetter(account.email()));
        avatar.getStyleClass().add("saved-account-avatar");

        VBox meta = new VBox(2);
        Label emailLabel = new Label(account.email());
        emailLabel.getStyleClass().add("saved-account-email");
        Label hintLabel = new Label("Windows Hello required");
        hintLabel.getStyleClass().add("saved-account-hint");
        meta.getChildren().addAll(emailLabel, hintLabel);

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().addAll("btn-primary", "auth-primary-btn", "saved-account-login-btn");
        loginButton.setOnAction(e -> quickLogin(account.email()));

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().add("btn-ghost");
        removeButton.setOnAction(e -> {
            savedAccountService.remove(account.email());
            refreshSavedAccountsUi();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        card.getChildren().addAll(avatar, meta, spacer, loginButton, removeButton);
        return card;
    }

    private String extractAvatarLetter(String emailValue) {
        String value = emailValue == null ? "" : emailValue.trim();
        if (value.isBlank()) {
            return "?";
        }
        return value.substring(0, 1).toUpperCase();
    }

    private void quickLogin(String accountEmail) {
        hideError();
        if (!verifyCaptchaOrShowError()) {
            shake(cardPane);
            return;
        }

        WindowsHelloAuthService.VerificationResult helloResult = WindowsHelloAuthService
                .verify("Authenticate to continue with EduCampus Quick Login");
        if (!helloResult.success()) {
            showError(helloResult.message());
            shake(cardPane);
            return;
        }

        String savedPassword = savedAccountService.loadPassword(accountEmail);
        if (savedPassword.isBlank()) {
            showError("Saved account unavailable. Use manual login.");
            shake(cardPane);
            return;
        }

        AuthUser user = authenticateOrShowError(accountEmail, savedPassword);
        if (user == null) {
            shake(cardPane);
            return;
        }

        completeLogin(user);
    }

    private void setManualLoginVisible(boolean showManual) {
        if (manualLoginPane != null) {
            manualLoginPane.setManaged(showManual);
            manualLoginPane.setVisible(showManual);
        }
        if (quickLoginPane != null) {
            boolean showQuick = !showManual && savedAccountsList != null && !savedAccountsList.getChildren().isEmpty();
            quickLoginPane.setManaged(showQuick);
            quickLoginPane.setVisible(showQuick);
        }
    }

    @FXML
    private void useManualLogin(ActionEvent event) {
        setManualLoginVisible(true);
    }

    @FXML
    private void useSavedAccountLogin(ActionEvent event) {
        setManualLoginVisible(false);
    }

    private void maybeOfferCredentialSave(String mail, String pass) {
        if (mail == null || mail.isBlank() || pass == null || pass.isBlank()) {
            return;
        }
        if (savedAccountService.containsEmail(mail)) {
            return;
        }

        Alert prompt = new Alert(Alert.AlertType.CONFIRMATION);
        prompt.setTitle("Quick Login");
        prompt.setHeaderText("Save account for faster login?");
        prompt.setContentText("Do you want to save your email and password for faster login next time?");
        prompt.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> decision = prompt.showAndWait();
        if (decision.isEmpty() || decision.get() != ButtonType.YES) {
            return;
        }

        SavedAccountService.SaveResult result = savedAccountService.save(mail, pass, null);
        if (result.invalidInput()) {
            showError("Unable to save credentials.");
            return;
        }

        if (result.limitReached()) {
            handleLimitReachedDuringSave(mail, pass, result.accounts());
            return;
        }

        refreshSavedAccountsUi();
    }

    private void handleLimitReachedDuringSave(String mail, String pass,
            List<SavedAccountService.SavedAccountEntry> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            showError("Saved account limit reached.");
            return;
        }

        List<String> options = new java.util.ArrayList<>();
        for (SavedAccountService.SavedAccountEntry account : accounts) {
            options.add("Replace " + account.email());
        }
        for (SavedAccountService.SavedAccountEntry account : accounts) {
            options.add("Remove " + account.email());
        }
        options.add("Keep current saved accounts");

        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("Saved accounts limit reached");
        dialog.setHeaderText("Maximum 2 saved accounts");
        dialog.setContentText("Choose an action:");

        Optional<String> picked = dialog.showAndWait();
        if (picked.isEmpty()) {
            return;
        }

        String choice = picked.get();
        if (choice.startsWith("Replace ")) {
            String replaceEmail = choice.substring("Replace ".length()).trim();
            SavedAccountService.SaveResult replaceResult = savedAccountService.save(mail, pass, replaceEmail);
            if (!replaceResult.saved()) {
                showError("Unable to replace saved account.");
                return;
            }
            refreshSavedAccountsUi();
            return;
        }

        if (choice.startsWith("Remove ")) {
            String removeEmail = choice.substring("Remove ".length()).trim();
            savedAccountService.remove(removeEmail);
            refreshSavedAccountsUi();
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

    private void initCaptcha() {
        recaptchaSecretKey = getConfigValue("RECAPTCHA_SECRET_KEY");
        String recaptchaSiteKey = getConfigValue("RECAPTCHA_SITE_KEY");

        if (captchaView == null) {
            return;
        }
        captchaView.setMinHeight(CAPTCHA_MIN_HEIGHT);
        captchaView.setPrefHeight(CAPTCHA_MIN_HEIGHT);
        if (recaptchaSiteKey.isBlank()) {
            captchaView.setManaged(false);
            captchaView.setVisible(false);
            showError("Captcha indisponible: clé site manquante.");
            return;
        }

        WebEngine engine = captchaView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                try {
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.setMember("captchaBridge", new CaptchaBridge(this));
                } catch (Exception ignored) {
                }
            }
        });
        try {
            String localUrl = ensureCaptchaServerRunning(recaptchaSiteKey);
            engine.load(localUrl);
        } catch (Exception e) {
            showError("Captcha indisponible: serveur local introuvable.");
        }
    }

    private boolean verifyCaptchaOrShowError() {
        String token = resolveCaptchaToken();
        if (token.isBlank()) {
            showError("Veuillez valider le captcha.");
            return false;
        }
        if (recaptchaSecretKey == null || recaptchaSecretKey.isBlank()) {
            showError("Captcha non configuré: clé secrète manquante.");
            return false;
        }

        boolean valid;
        try {
            valid = verifyCaptchaToken(token);
        } catch (Exception e) {
            showError("Vérification captcha impossible. Réessayez.");
            return false;
        } finally {
            captchaToken = "";
            resetCaptchaWidget();
        }

        if (!valid) {
            showError("Captcha invalide. Réessayez.");
            return false;
        }
        return true;
    }

    private String resolveCaptchaToken() {
        String tokenFromBridge = captchaToken == null ? "" : captchaToken.trim();
        if (!tokenFromBridge.isBlank()) {
            return tokenFromBridge;
        }
        if (captchaView == null) {
            return "";
        }
        try {
            Object value = captchaView.getEngine().executeScript(
                    "(function(){"
                            + "try {"
                            + "if (window.grecaptcha && typeof window.grecaptcha.getResponse === 'function') {"
                            + "var direct = window.grecaptcha.getResponse();"
                            + "if (direct) { return direct; }"
                            + "}"
                            + "var el = document.getElementById('g-recaptcha-response');"
                            + "return el && el.value ? el.value : '';"
                            + "} catch (e) { return ''; }"
                            + "})()");
            String tokenFromPage = value == null ? "" : String.valueOf(value).trim();
            if (!tokenFromPage.isBlank()) {
                captchaToken = tokenFromPage;
                return tokenFromPage;
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private boolean verifyCaptchaToken(String token) throws Exception {
        String body = "secret=" + URLEncoder.encode(recaptchaSecretKey, StandardCharsets.UTF_8)
                + "&response=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://www.google.com/recaptcha/api/siteverify"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(java.time.Duration.ofSeconds(8))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return false;
        }
        String payload = response.body() == null ? "" : response.body();
        return CAPTCHA_SUCCESS_PATTERN.matcher(payload).find();
    }

    private void resetCaptchaWidget() {
        if (captchaView == null) {
            return;
        }
        try {
            captchaView.getEngine().executeScript("if (window.grecaptcha) { window.grecaptcha.reset(); }");
        } catch (Exception ignored) {
        }
    }

    private void onCaptchaToken(String token) {
        captchaToken = token == null ? "" : token.trim();
    }

    private void onCaptchaExpired() {
        captchaToken = "";
    }

    private void onCaptchaResize(double requestedHeight) {
        if (captchaView == null) {
            return;
        }
        double nextHeight = Math.max(CAPTCHA_MIN_HEIGHT, Math.min(CAPTCHA_MAX_HEIGHT, requestedHeight));
        captchaView.setPrefHeight(nextHeight);
        captchaView.setMinHeight(Math.min(CAPTCHA_MIN_HEIGHT, nextHeight));
    }

    private String getConfigValue(String key) {
        String fromProperty = String.valueOf(System.getProperty(key, "")).trim();
        if (!fromProperty.isBlank()) {
            return fromProperty;
        }

        String fromEnv = String.valueOf(System.getenv(key) == null ? "" : System.getenv(key)).trim();
        if (!fromEnv.isBlank()) {
            return fromEnv;
        }

        String fromDotEnv = loadDotEnvValues().getOrDefault(key, "").trim();
        return fromDotEnv;
    }

    private static Map<String, String> loadDotEnvValues() {
        Map<String, String> cache = dotEnvValues;
        if (cache != null) {
            return cache;
        }

        Map<String, String> parsed = new HashMap<>();
        for (Path candidate : dotenvCandidates()) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            try {
                List<String> lines = Files.readAllLines(candidate, StandardCharsets.UTF_8);
                for (String rawLine : lines) {
                    if (rawLine == null) {
                        continue;
                    }
                    String line = rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eq = line.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    String name = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    if (!name.isEmpty()) {
                        parsed.put(name, stripOptionalQuotes(value));
                    }
                }
                break;
            } catch (Exception ignored) {
            }
        }

        dotEnvValues = Map.copyOf(parsed);
        return dotEnvValues;
    }

    private static List<Path> dotenvCandidates() {
        return List.of(
                Paths.get(".env"),
                Paths.get("eduCompus-javafx", ".env"),
                Paths.get("..", "eduCompus-javafx", ".env"));
    }

    private static String stripOptionalQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value == null ? "" : value;
        }
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String ensureCaptchaServerRunning(String siteKey) throws IOException {
        synchronized (CAPTCHA_SERVER_LOCK) {
            captchaSiteKey = siteKey == null ? "" : siteKey.trim();
            if (captchaLocalServer != null && captchaLocalServerUrl != null && !captchaLocalServerUrl.isBlank()) {
                return captchaLocalServerUrl;
            }

            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/captcha", FrontLoginController::handleCaptchaRequest);
            server.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "educompus-captcha-local-server");
                t.setDaemon(true);
                return t;
            }));
            server.start();

            captchaLocalServer = server;
            captchaLocalServerUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/captcha";
            return captchaLocalServerUrl;
        }
    }

    private static void handleCaptchaRequest(HttpExchange exchange) throws IOException {
        if (exchange == null) {
            return;
        }

        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String html = buildCaptchaHtml(captchaSiteKey);
            byte[] payload = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");
            exchange.sendResponseHeaders(200, payload.length);
            try (var os = exchange.getResponseBody()) {
                os.write(payload);
            }
        } finally {
            exchange.close();
        }
    }

    private static String buildCaptchaHtml(String siteKey) {
        String safeSiteKey = siteKey == null ? "" : siteKey.replace("\"", "");
        return "<!doctype html>"
                + "<html><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<style>html,body{margin:0;padding:0;background:transparent;overflow:hidden;}"
                + "body{display:flex;align-items:center;justify-content:center;height:100%;}</style>"
                + "<script>"
                + "function onCaptchaSolved(token){if(window.captchaBridge){window.captchaBridge.onSuccess(token);}}"
                + "function onCaptchaExpired(){if(window.captchaBridge){window.captchaBridge.onExpired();}}"
                + "function notifyHeight(){"
                + "var h=234;"
                + "try{"
                + "h=Math.max(h,document.body?document.body.scrollHeight:0,document.documentElement?document.documentElement.scrollHeight:0);"
                + "var ifr=document.getElementsByTagName('iframe');"
                + "for(var i=0;i<ifr.length;i++){"
                + "var r=ifr[i].getBoundingClientRect();"
                + "h=Math.max(h,r.bottom+8);"
                + "}"
                + "}catch(e){}"
                + "if(window.captchaBridge){window.captchaBridge.onResize(h);}"
                + "}"
                + "document.addEventListener('DOMContentLoaded',function(){"
                + "notifyHeight();"
                + "setTimeout(notifyHeight,300);"
                + "setTimeout(notifyHeight,900);"
                + "if(window.MutationObserver){"
                + "var obs=new MutationObserver(function(){notifyHeight();});"
                + "obs.observe(document.documentElement||document.body,{childList:true,subtree:true,attributes:true});"
                + "}"
                + "setInterval(notifyHeight,250);"
                + "});"
                + "</script>"
                + "<script src='https://www.google.com/recaptcha/api.js' async defer></script>"
                + "</head><body>"
                + "<div class='g-recaptcha' data-sitekey='" + safeSiteKey
                + "' data-callback='onCaptchaSolved' data-expired-callback='onCaptchaExpired'></div>"
                + "</body></html>";
    }

    public static final class CaptchaBridge {
        private final FrontLoginController controller;

        public CaptchaBridge(FrontLoginController controller) {
            this.controller = controller;
        }

        public void onSuccess(String token) {
            if (controller == null) {
                return;
            }
            Platform.runLater(() -> controller.onCaptchaToken(token));
        }

        public void onExpired() {
            if (controller == null) {
                return;
            }
            Platform.runLater(controller::onCaptchaExpired);
        }

        public void onResize(double height) {
            if (controller == null) {
                return;
            }
            Platform.runLater(() -> controller.onCaptchaResize(height));
        }
    }

    private String summarizeThrowable(Throwable t) {
        if (t == null) {
            return "unknown error";
        }
        if (t instanceof IllegalStateException && t.getMessage() != null
                && t.getMessage().contains("Failed to load FXML:")) {
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
        String fullName = signupName == null ? "" : String.valueOf(signupName.getText()).trim();
        String mail = signupEmail == null ? "" : String.valueOf(signupEmail.getText()).trim().toLowerCase();
        String p1 = signupPassword == null ? "" : String.valueOf(signupPassword.getText());
        String p2 = signupPassword2 == null ? "" : String.valueOf(signupPassword2.getText());
        if (fullName.isBlank() || mail.isBlank() || p1.isBlank() || p2.isBlank()) {
            if (signupInfoLabel != null) {
                signupInfoLabel.setText("Veuillez remplir tous les champs.");
            }
            shake(cardPane);
            return;
        }
        if (!p1.equals(p2)) {
            if (signupInfoLabel != null) {
                signupInfoLabel.setText("Les mots de passe ne correspondent pas.");
            }
            shake(cardPane);
            return;
        }
        if (p1.length() < 6) {
            if (signupInfoLabel != null) {
                signupInfoLabel.setText("Le mot de passe doit contenir au moins 6 caracteres.");
            }
            shake(cardPane);
            return;
        }
        try {
            DbAuthService.registerUser(fullName, mail, p1);
        } catch (Exception e) {
            if (signupInfoLabel != null) {
                signupInfoLabel.setText("Inscription impossible: " + summarizeThrowable(e));
            }
            shake(cardPane);
            return;
        }
        if (signupInfoLabel != null) {
            signupInfoLabel.setText("Compte cree en base. Vous pouvez vous connecter.");
        }
        if (email != null) {
            email.setText(mail);
        }
        if (password != null) {
            password.clear();
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

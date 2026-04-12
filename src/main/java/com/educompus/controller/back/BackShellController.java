package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.nav.Navigator;
import com.educompus.util.Theme;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.css.PseudoClass;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public final class BackShellController {
    private static final String ICON_USER =
            "M12 12C14.76 12 17 9.76 17 7S14.76 2 12 2 7 4.24 7 7 9.24 12 12 12Z"
                    + "M12 14C8.13 14 5 16.13 5 18.75V21H19V18.75C19 16.13 15.87 14 12 14Z";

    private static final String ICON_ADMIN =
            "M12 1L3 5V11C3 16.55 6.84 21.74 12 23C17.16 21.74 21 16.55 21 11V5L12 1Z";
    private static final String ICON_TEACHER =
            "M12 2L1 7L12 12L21 7.91V13H23V7L12 2Z"
                    + "M6 10.55V15.5C6 17.99 8.69 20 12 20S18 17.99 18 15.5V10.55L12 13.28L6 10.55Z";

    @FXML
    private BorderPane shell;

    @FXML
    private StackPane contentWrap;

    @FXML
    private Button userMenuBtn;

    @FXML
    private StackPane userAvatar;

    @FXML
    private SVGPath userAvatarSvg;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label topbarBrandLabel;

    private ContextMenu userMenuPopup;
    private Label headerEmailLabel;
    private final PseudoClass pcOn = PseudoClass.getPseudoClass("on");

    @FXML
    private Button navDashboardBtn;

    @FXML
    private Button navUsersBtn;

    @FXML
    private Button navCoursesBtn;

    @FXML
    private Button navCategoriesBtn;

    @FXML
    private Button navSessionsBtn;

    @FXML
    private Button navExamsBtn;

    @FXML
    private Button navProjectsBtn;

    @FXML
    private Button navClubsBtn;

    @FXML
    private Button navEventsBtn;

    @FXML
    private Button navMarketplaceBtn;

    @FXML
    private Button navPaymentsBtn;

    @FXML
    private Button navSupportBtn;

    @FXML
    private Button navReportsBtn;

    @FXML
    private Button navSettingsBtn;

    private final List<Button> navButtons = new ArrayList<>();

    @FXML
    private void initialize() {
        if (!(AppState.isAdmin() || AppState.isTeacher())) {
            Platform.runLater(() -> {
                try {
                    Navigator.goRoot("View/front/FrontLogin.fxml");
                } catch (Exception ignored) {
                }
            });
            return;
        }

        String mail = AppState.getUserEmail();
        if (mail == null || mail.isBlank()) {
            mail = "admin@educompus.tn";
        }

        String display = AppState.getUserDisplayName();
        String fallbackLabel = AppState.isTeacher() ? "Teacher" : "Admin";
        display = display == null || display.isBlank() ? toDisplayName(mail, fallbackLabel) : display;

        if (userNameLabel != null) {
            userNameLabel.setText(display);
        }
        if (topbarBrandLabel != null && AppState.isTeacher()) {
            topbarBrandLabel.setText("Teachers EduCampus");
        }
        applyAvatarIcon();

        buildUserMenuPopup(display, mail);

        navButtons.add(navDashboardBtn);
        navButtons.add(navUsersBtn);
        navButtons.add(navCoursesBtn);
        navButtons.add(navCategoriesBtn);
        navButtons.add(navSessionsBtn);
        navButtons.add(navExamsBtn);
        navButtons.add(navProjectsBtn);
        navButtons.add(navClubsBtn);
        navButtons.add(navEventsBtn);
        navButtons.add(navMarketplaceBtn);
        navButtons.add(navPaymentsBtn);
        navButtons.add(navSupportBtn);
        navButtons.add(navReportsBtn);
        navButtons.add(navSettingsBtn);

        Theme.apply(shell);
        setContent(safeLoad("View/back/BackDashboard.fxml"));
        setActive(navDashboardBtn);
    }

    @FXML
    private void navDashboard(ActionEvent event) {
        setContent(safeLoad("View/back/BackDashboard.fxml"));
        setActive(navDashboardBtn);
    }

    @FXML
    private void navUsers(ActionEvent event) {
        setContent(safeLoad("View/back/BackUsers.fxml"));
        setActive(navUsersBtn);
    }

    @FXML
    private void navCourses(ActionEvent event) {
        setContent(safeLoad("View/back/BackCourses.fxml"));
        setActive(navCoursesBtn);
    }

    @FXML
    private void navCategories(ActionEvent event) {
        setContent(safeLoad("View/back/BackCategories.fxml"));
        setActive(navCategoriesBtn);
    }

    @FXML
    private void navSessions(ActionEvent event) {
        setContent(safeLoad("View/back/BackSessions.fxml"));
        setActive(navSessionsBtn);
    }

    @FXML
    private void navExams(ActionEvent event) {
        setContent(safeLoad("View/back/BackExamsCatalogue.fxml"));
        setActive(navExamsBtn);
    }

    @FXML
    private void navProjects(ActionEvent event) {
        setContent(safeLoad("View/back/BackProjects.fxml"));
        setActive(navProjectsBtn);
    }

    @FXML
    private void navClubs(ActionEvent event) {
        setContent(safeLoad("View/back/BackClubs.fxml"));
        setActive(navClubsBtn);
    }

    @FXML
    private void navEvents(ActionEvent event) {
        setContent(safeLoad("View/back/BackEvents.fxml"));
        setActive(navEventsBtn);
    }

    @FXML
    private void navMarketplace(ActionEvent event) {
        setContent(safeLoad("View/back/BackMarketplace.fxml"));
        setActive(navMarketplaceBtn);
    }

    @FXML
    private void navPayments(ActionEvent event) {
        setContent(safeLoad("View/back/BackPayments.fxml"));
        setActive(navPaymentsBtn);
    }

    @FXML
    private void navSupport(ActionEvent event) {
        setContent(safeLoad("View/back/BackSupport.fxml"));
        setActive(navSupportBtn);
    }

    @FXML
    private void navReports(ActionEvent event) {
        setContent(safeLoad("View/back/BackReports.fxml"));
        setActive(navReportsBtn);
    }

    @FXML
    private void navSettings(ActionEvent event) {
        setContent(safeLoad("View/back/BackSettings.fxml"));
        setActive(navSettingsBtn);
    }

    private void setContent(Node node) {
        if (contentWrap == null) {
            shell.setCenter(node);
            return;
        }
        if (node == null) {
            contentWrap.getChildren().clear();
            return;
        }
        node.setOpacity(0.0);
        contentWrap.getChildren().setAll(node);
        FadeTransition ft = new FadeTransition(Duration.millis(160), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    @FXML
    private void signOut(ActionEvent event) {
        if (userMenuPopup != null) {
            userMenuPopup.hide();
        }
        AppState.setRole(AppState.Role.USER);
        AppState.setUserEmail("");
        AppState.setUserDisplayName("");
        AppState.setUserImageUrl("");
        Navigator.goRoot("View/front/FrontLogin.fxml");
    }

    @FXML
    private void toggleUserMenu(ActionEvent event) {
        if (userMenuBtn == null || userMenuPopup == null) {
            return;
        }
        if (userMenuPopup.isShowing()) {
            userMenuPopup.hide();
            return;
        }

        Bounds b = userMenuBtn.localToScreen(userMenuBtn.getBoundsInLocal());
        if (b == null) {
            userMenuPopup.show(userMenuBtn, Side.BOTTOM, 0, 6);
            return;
        }
        userMenuPopup.show(userMenuBtn, b.getMinX(), b.getMaxY() + 6);
    }

    private void setActive(Button active) {
        for (Button button : navButtons) {
            if (button == null) {
                continue;
            }
            button.getStyleClass().remove("active");
        }

        if (active != null && !active.getStyleClass().contains("active")) {
            active.getStyleClass().add("active");
        }
    }

    private String toDisplayName(String email, String fallback) {
        String mail = email == null ? "" : email.trim();
        if (mail.isBlank()) {
            return fallback;
        }
        int at = mail.indexOf('@');
        if (at > 0) {
            return mail.substring(0, at);
        }
        return mail;
    }

    private void applyAvatarIcon() {
        if (userAvatarSvg == null) {
            return;
        }
        boolean isAdmin = AppState.isAdmin();
        boolean isTeacher = AppState.isTeacher();
        String imageUrl = AppState.getUserImageUrl();

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                ImageView iv = new ImageView(new javafx.scene.image.Image(imageUrl, 30, 30, true, true));
                iv.setFitWidth(30);
                iv.setFitHeight(30);
                userAvatar.getChildren().setAll(iv);
            } catch (Exception ignored) {
                // fallback to SVG
            }
        } else {
            if (isAdmin) {
                userAvatarSvg.setContent(ICON_ADMIN);
            } else if (isTeacher) {
                userAvatarSvg.setContent(ICON_TEACHER);
            } else {
                userAvatarSvg.setContent(ICON_USER);
            }
            if (!userAvatar.getChildren().contains(userAvatarSvg)) {
                userAvatar.getChildren().setAll(userAvatarSvg);
            }
        }

        if (userAvatar != null) {
            userAvatar.getStyleClass().removeAll("admin", "teacher");
            if (isAdmin) {
                userAvatar.getStyleClass().add("admin");
            } else if (isTeacher) {
                userAvatar.getStyleClass().add("teacher");
            }
        }
    }

    private void buildUserMenuPopup(String displayName, String email) {
        userMenuPopup = new ContextMenu();
        userMenuPopup.getStyleClass().add("user-menu-popup");
        userMenuPopup.setAutoHide(true);

        Label iconUser = new Label("\u25CF");
        iconUser.getStyleClass().add("menu-icon");

        String fallbackTitle = AppState.isTeacher() ? "Teacher" : "Admin";
        Label title = new Label(displayName == null || displayName.isBlank() ? fallbackTitle : displayName);
        title.getStyleClass().add("user-menu-title");

        headerEmailLabel = new Label(email == null ? "" : email);
        headerEmailLabel.getStyleClass().add("user-menu-subtitle");

        VBox headerText = new VBox(2, title, headerEmailLabel);
        HBox header = new HBox(10, iconUser, headerText);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("user-menu-header");

        Label darkIcon = new Label(AppState.isDark() ? "\u263E" : "\u263C");
        darkIcon.getStyleClass().add("menu-icon");

        Label darkLabel = new Label("Dark mode");
        darkLabel.getStyleClass().add("user-menu-row-text");

        StackPane darkSwitch = new StackPane();
        darkSwitch.getStyleClass().add("switch");
        darkSwitch.setMinSize(46, 24);
        darkSwitch.setPrefSize(46, 24);
        darkSwitch.setMaxSize(46, 24);

        Region thumb = new Region();
        thumb.getStyleClass().add("switch-thumb");
        darkSwitch.getChildren().add(thumb);
        applySwitchState(darkSwitch, thumb, AppState.isDark());

        darkSwitch.setOnMouseClicked(e -> {
            boolean v = !AppState.isDark();
            AppState.setDark(v);
            applySwitchState(darkSwitch, thumb, v);
            darkIcon.setText(v ? "\u263E" : "\u263C");
            Theme.apply(shell);
        });

        Region grow = new Region();
        HBox.setHgrow(grow, javafx.scene.layout.Priority.ALWAYS);
        HBox darkRow = new HBox(10, darkIcon, darkLabel, grow, darkSwitch);
        darkRow.setAlignment(Pos.CENTER_LEFT);
        darkRow.getStyleClass().add("user-menu-row");

        Button signOutBtn = new Button("\u23FB");
        signOutBtn.getStyleClass().add("user-menu-signout");
        signOutBtn.setOnAction(this::signOut);
        signOutBtn.setFocusTraversable(false);

        StackPane signOutWrap = new StackPane(signOutBtn);
        signOutWrap.setPadding(new Insets(10, 0, 6, 0));
        signOutWrap.setAlignment(Pos.CENTER);

        VBox content = new VBox(10, header, new Separator(), darkRow, new Separator(), signOutWrap);
        content.setPadding(new Insets(12));

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setPrefViewportHeight(240);
        sp.getStyleClass().add("user-menu-scroll");

        CustomMenuItem root = new CustomMenuItem(sp, false);
        root.setHideOnClick(false);
        userMenuPopup.getItems().setAll(root);
    }

    private void applySwitchState(StackPane sw, Region thumb, boolean on) {
        if (sw != null) {
            sw.pseudoClassStateChanged(pcOn, on);
        }
        if (sw != null && thumb != null) {
            StackPane.setAlignment(thumb, on ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            thumb.setTranslateX(on ? -2 : 2);
        }
    }

    private Node safeLoad(String fxmlPath) {
        try {
            return Navigator.load(fxmlPath);
        } catch (Exception e) {
            e.printStackTrace();

            Label title = new Label("Erreur interface");
            title.getStyleClass().addAll("page-title");

            Label details = new Label(String.valueOf(e.getMessage()));
            details.getStyleClass().addAll("page-subtitle");
            details.setWrapText(true);

            VBox box = new VBox(10, title, details);
            box.getStyleClass().add("content");
            box.setPadding(new Insets(22));
            return box;
        }
    }
}

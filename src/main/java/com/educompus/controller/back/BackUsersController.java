package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.model.AuthUser;
import com.educompus.nav.Navigator;
import com.educompus.service.AuthUserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Optional;

public class BackUsersController {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> roleFilter;
    @FXML
    private ComboBox<String> sortBy;
    @FXML
    private Label lblResults;

    @FXML
    private Label statTotal;
    @FXML
    private Label statAdmins;
    @FXML
    private Label statTeachers;
    @FXML
    private Label statStandard;
    @FXML
    private Label statMixed;
    @FXML
    private Label statAvatar;

    @FXML
    private ListView<AuthUser> userList;

    private final AuthUserService service = new AuthUserService();
    private final ObservableList<AuthUser> data = FXCollections.observableArrayList();
    private FilteredList<AuthUser> filtered;
    private SortedList<AuthUser> sorted;

    @FXML
    private void initialize() {
        userList.setCellFactory(list -> new ListCell<>() {
            private final ImageView avatarView = new ImageView();
            private final Label nameLabel = new Label();
            private final Label emailLabel = new Label();
            private final VBox identityBox = new VBox(2, nameLabel, emailLabel);
            private final Label roleChip = new Label();
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox actionsBox = new HBox(6, btnEdit, btnDelete);
            private final Region spacer = new Region();
            private final HBox container = new HBox(12, avatarView, identityBox, roleChip, spacer, actionsBox);

            {
                container.setAlignment(Pos.CENTER_LEFT);
                container.setPadding(new Insets(8, 10, 8, 10));
                avatarView.setFitWidth(32);
                avatarView.setFitHeight(32);
                avatarView.setPreserveRatio(true);
                Circle clip = new Circle(16, 16, 16);
                avatarView.setClip(clip);

                nameLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: -edu-text;");
                emailLabel.getStyleClass().add("page-subtitle");

                roleChip.getStyleClass().add("chip");
                roleChip.setPadding(new Insets(4, 10, 4, 10));

                btnEdit.getStyleClass().add("btn-icon");
                btnDelete.getStyleClass().add("btn-icon");
                actionsBox.setAlignment(Pos.CENTER);

                HBox.setHgrow(spacer, Priority.ALWAYS);

                SVGPath editIcon = new SVGPath();
                editIcon.setContent(
                        "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1.003 1.003 0 0 0 0-1.42l-2.34-2.34a1.003 1.003 0 0 0-1.42 0l-1.83 1.83 3.75 3.75 1.84-1.82z");
                editIcon.setScaleX(0.8);
                editIcon.setScaleY(0.8);
                editIcon.setFill(Color.web("#066ac9"));
                btnEdit.setGraphic(editIcon);
                btnEdit.setTooltip(new Tooltip("Modifier l'utilisateur"));

                SVGPath deleteIcon = new SVGPath();
                deleteIcon.setContent("M6 19c0 1.1.9 2 2 2h8a2 2 0 0 0 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
                deleteIcon.setScaleX(0.8);
                deleteIcon.setScaleY(0.8);
                deleteIcon.setFill(Color.web("#e11d48"));
                btnDelete.setGraphic(deleteIcon);
                btnDelete.setTooltip(new Tooltip("Supprimer l'utilisateur"));
            }

            @Override
            protected void updateItem(AuthUser user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                    return;
                }
                nameLabel.setText(user.displayName());
                emailLabel.setText(user.email());

                roleChip.setText(roleLabel(user));
                roleChip.getStyleClass().removeAll("chip-success", "chip-warning", "chip-info");
                if (user.admin() && user.teacher()) {
                    roleChip.getStyleClass().add("chip-warning");
                } else if (user.admin()) {
                    roleChip.getStyleClass().add("chip-info");
                } else if (user.teacher()) {
                    roleChip.getStyleClass().add("chip-success");
                }

                btnEdit.setOnAction(e -> openEditDialog(user));
                btnDelete.setOnAction(e -> deleteUser(user));

                String url = user.imageUrl() != null && !user.imageUrl().isBlank() ? user.imageUrl() : null;
                try {
                    if (url != null) {
                        avatarView.setImage(new Image(url, true));
                    } else {
                        // Fallback generic user icon or letter
                        avatarView.setImage(null);
                    }
                } catch (Exception e) {
                    avatarView.setImage(null);
                }
                setGraphic(container);
            }
        });

        roleFilter.getItems().setAll("Tous les roles", "Administrateurs", "Enseignants", "Administrateur + Enseignant",
                "Utilisateurs");
        roleFilter.getSelectionModel().selectFirst();

        sortBy.getItems().setAll(
                "Nom (A-Z)",
                "Nom (Z-A)",
                "Email (A-Z)",
                "Role (admin -> user)",
                "Plus recent",
                "Plus ancien");
        sortBy.getSelectionModel().select("Plus recent");

        filtered = new FilteredList<>(data, u -> true);
        sorted = new SortedList<>(filtered);
        userList.setItems(sorted);

        loadUsers();
    }

    @FXML
    private void onImportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner le fichier CSV des utilisateurs");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));

        File file = fileChooser.showOpenDialog(userList.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            FXMLLoader loader = Navigator.loader("View/back/BackUserImport.fxml");
            Parent root = loader.load();
            BackUserImportController controller = loader.getController();
            controller.loadFile(file);
            controller.setOnSuccess(this::loadUsers);

            Stage stage = new Stage();
            stage.setTitle("Aperçu de l'importation");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            if (userList.getScene() != null) {
                scene.getStylesheets().addAll(userList.getScene().getStylesheets());
            }
            stage.setScene(scene);
            stage.setMinWidth(920);
            stage.setMinHeight(600);
            stage.showAndWait();
        } catch (Exception e) {
            showError("Impossible d'ouvrir l'interface d'importation.", e);
        }
    }

    @FXML
    private void onExportCSV() {
        if (data.isEmpty()) {
            showSimpleAlert(Alert.AlertType.INFORMATION, "Export", "Aucune donnée à exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        fileChooser.setInitialFileName("utilisateurs_educampus.csv");

        File file = fileChooser.showSaveDialog(userList.getScene().getWindow());
        if (file == null) {
            return;
        }

        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            // BOM for Excel UTF-8 support
            writer.write('\ufeff');
            writer.println("ID;Nom;Email;password;Rôle;Admin;Enseignant");
            for (AuthUser u : filtered) {
                writer.println(String.format("%d;%s;%s;%s;%s;%s",
                        u.id(),
                        u.displayName(),
                        u.email(),
                        roleLabel(u),
                        u.admin() ? "Oui" : "Non",
                        u.teacher() ? "Oui" : "Non"));
            }
            showSimpleAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Données exportées avec succès vers :\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showError("Échec de l'exportation.", e);
        }
    }

    @FXML
    private void onSearch() {
        applyFilters();
    }

    @FXML
    private void onRoleFilter() {
        applyFilters();
    }

    @FXML
    private void onSortChanged() {
        applySort();
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        roleFilter.getSelectionModel().selectFirst();
        sortBy.getSelectionModel().select("Plus recent");
        applyFilters();
        applySort();
    }

    @FXML
    private void onAddUser() {
        try {
            FXMLLoader loader = Navigator.loader("View/back/BackUserForm.fxml");
            Parent root = loader.load();
            BackUserFormController controller = loader.getController();
            controller.prepareCreate();
            controller.setOnSuccess(this::loadUsers);

            Stage stage = new Stage();
            stage.setTitle("Ajouter un utilisateur");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            if (userList.getScene() != null) {
                scene.getStylesheets().addAll(userList.getScene().getStylesheets());
            }
            stage.setScene(scene);
            stage.setMinWidth(620);
            stage.setMinHeight(500);
            stage.showAndWait();
        } catch (Exception e) {
            showError("Impossible d'ouvrir le formulaire utilisateur.", e);
        }
    }

    private void openEditDialog(AuthUser user) {
        try {
            FXMLLoader loader = Navigator.loader("View/back/BackUserForm.fxml");
            Parent root = loader.load();
            BackUserFormController controller = loader.getController();
            controller.prepareUpdate(user);
            controller.setOnSuccess(this::loadUsers);

            Stage stage = new Stage();
            stage.setTitle("Modifier un utilisateur");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            if (userList.getScene() != null) {
                scene.getStylesheets().addAll(userList.getScene().getStylesheets());
            }
            stage.setScene(scene);
            stage.setMinWidth(620);
            stage.setMinHeight(500);
            stage.showAndWait();
        } catch (Exception e) {
            showError("Impossible d'ouvrir le formulaire de modification.", e);
        }
    }

    private void deleteUser(AuthUser user) {
        if (user == null) {
            return;
        }
        if (user.id() == AppState.getUserId()) {
            showSimpleAlert(Alert.AlertType.WARNING, "Suppression bloquee",
                    "Vous ne pouvez pas supprimer votre propre compte en cours.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer l'utilisateur " + user.displayName() + " ?");
        confirm.setContentText("Cette action est irreversible.");
        styleAlert(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            service.delete(user.id());
            loadUsers();
        } catch (Exception e) {
            showError("Suppression impossible.", e);
        }
    }

    private void loadUsers() {
        try {
            data.setAll(service.findAll());
            applyFilters();
            applySort();
            refreshStats();
        } catch (SQLException e) {
            showError("Impossible de charger les utilisateurs.", e);
        }
    }

    private void refreshStats() {
        AuthUserService.UserStats stats = service.buildStats(data);
        statTotal.setText(String.valueOf(stats.total()));
        statAdmins.setText(String.valueOf(stats.admins()));
        statTeachers.setText(String.valueOf(stats.teachers()));
        statStandard.setText(String.valueOf(stats.standard()));
        statMixed.setText(String.valueOf(stats.mixed()));
        statAvatar.setText(String.valueOf(stats.withAvatar()));
    }

    private void applyFilters() {
        String term = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedRole = roleFilter.getValue() == null ? "Tous les roles" : roleFilter.getValue();

        filtered.setPredicate(user -> {
            if (user == null) {
                return false;
            }

            boolean textMatch = term.isBlank()
                    || user.displayName().toLowerCase().contains(term)
                    || user.email().toLowerCase().contains(term)
                    || roleLabel(user).toLowerCase().contains(term);

            boolean roleMatch = switch (selectedRole) {
                case "Administrateurs" -> user.admin() && !user.teacher();
                case "Enseignants" -> user.teacher() && !user.admin();
                case "Administrateur + Enseignant" -> user.admin() && user.teacher();
                case "Utilisateurs" -> !user.admin() && !user.teacher();
                default -> true;
            };

            return textMatch && roleMatch;
        });

        lblResults.setText(filtered.size() + " utilisateur(s) affiche(s)");
    }

    private void applySort() {
        String selected = sortBy.getValue() == null ? "Plus recent" : sortBy.getValue();
        Comparator<AuthUser> comparator = switch (selected) {
            case "Nom (A-Z)" -> Comparator.comparing(u -> safe(u.displayName()).toLowerCase());
            case "Nom (Z-A)" -> Comparator.comparing((AuthUser u) -> safe(u.displayName()).toLowerCase()).reversed();
            case "Email (A-Z)" -> Comparator.comparing(u -> safe(u.email()).toLowerCase());
            case "Role (admin -> user)" -> Comparator
                    .comparingInt(this::roleRank)
                    .thenComparing(u -> safe(u.displayName()).toLowerCase());
            case "Plus ancien" -> Comparator.comparingInt(AuthUser::id);
            default -> Comparator.comparingInt(AuthUser::id).reversed();
        };

        sorted.setComparator(comparator);
    }

    private int roleRank(AuthUser user) {
        if (user.admin() && user.teacher()) {
            return 0;
        }
        if (user.admin()) {
            return 1;
        }
        if (user.teacher()) {
            return 2;
        }
        return 3;
    }

    private String roleLabel(AuthUser user) {
        if (user == null) {
            return "-";
        }
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

    private void showError(String title, Exception e) {
        String details = e == null || e.getMessage() == null ? "Erreur inconnue" : e.getMessage();
        showSimpleAlert(Alert.AlertType.ERROR, "Erreur", title + "\n" + details);
    }

    private void showSimpleAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(header);
        alert.setHeaderText(header);
        alert.setContentText(message);
        styleAlert(alert);
        alert.showAndWait();
    }

    private void styleAlert(Alert alert) {
        if (userList.getScene() != null) {
            alert.getDialogPane().getStylesheets().addAll(userList.getScene().getStylesheets());
        }
    }
}

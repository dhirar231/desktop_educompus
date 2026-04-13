package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.model.AuthUser;
import com.educompus.nav.Navigator;
import com.educompus.service.AuthUserService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
    private TableView<AuthUser> tableUsers;
    @FXML
    private TableColumn<AuthUser, Number> colId;
    @FXML
    private TableColumn<AuthUser, String> colDisplayName;
    @FXML
    private TableColumn<AuthUser, String> colEmail;
    @FXML
    private TableColumn<AuthUser, String> colRole;
    @FXML
    private TableColumn<AuthUser, String> colActions;

    private final AuthUserService service = new AuthUserService();
    private final ObservableList<AuthUser> data = FXCollections.observableArrayList();
    private FilteredList<AuthUser> filtered;
    private SortedList<AuthUser> sorted;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().id()));
        colDisplayName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().displayName()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().email()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(roleLabel(c.getValue())));

        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().removeAll("role-admin", "role-teacher", "role-mixed", "role-user");
                    return;
                }
                setText(item);
                getStyleClass().removeAll("role-admin", "role-teacher", "role-mixed", "role-user");
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    return;
                }
                AuthUser user = getTableView().getItems().get(getIndex());
                if (user.admin() && user.teacher()) {
                    getStyleClass().add("role-mixed");
                } else if (user.admin()) {
                    getStyleClass().add("role-admin");
                } else if (user.teacher()) {
                    getStyleClass().add("role-teacher");
                } else {
                    getStyleClass().add("role-user");
                }
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox box = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("btn-ghost");
                btnDelete.getStyleClass().add("btn-ghost");
                box.setAlignment(Pos.CENTER);

                btnEdit.setOnAction(e -> openEditDialog(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> deleteUser(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        roleFilter.getItems().setAll("Tous les roles", "Administrateurs", "Enseignants", "Administrateur + Enseignant", "Utilisateurs");
        roleFilter.getSelectionModel().selectFirst();

        sortBy.getItems().setAll(
                "Nom (A-Z)",
                "Nom (Z-A)",
                "Email (A-Z)",
                "Role (admin -> user)",
                "Plus recent (id desc)",
                "Plus ancien (id asc)"
        );
        sortBy.getSelectionModel().select("Plus recent (id desc)");

        filtered = new FilteredList<>(data, u -> true);
        sorted = new SortedList<>(filtered);
        tableUsers.setItems(sorted);

        loadUsers();
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
        sortBy.getSelectionModel().select("Plus recent (id desc)");
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
            if (tableUsers.getScene() != null) {
                scene.getStylesheets().addAll(tableUsers.getScene().getStylesheets());
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
            if (tableUsers.getScene() != null) {
                scene.getStylesheets().addAll(tableUsers.getScene().getStylesheets());
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
            showSimpleAlert(Alert.AlertType.WARNING, "Suppression bloquee", "Vous ne pouvez pas supprimer votre propre compte en cours.");
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
                    || roleLabel(user).toLowerCase().contains(term)
                    || String.valueOf(user.id()).contains(term);

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
        String selected = sortBy.getValue() == null ? "Plus recent (id desc)" : sortBy.getValue();
        Comparator<AuthUser> comparator = switch (selected) {
            case "Nom (A-Z)" -> Comparator.comparing(u -> safe(u.displayName()).toLowerCase());
            case "Nom (Z-A)" -> Comparator.comparing((AuthUser u) -> safe(u.displayName()).toLowerCase()).reversed();
            case "Email (A-Z)" -> Comparator.comparing(u -> safe(u.email()).toLowerCase());
            case "Role (admin -> user)" -> Comparator
                    .comparingInt(this::roleRank)
                    .thenComparing(u -> safe(u.displayName()).toLowerCase());
            case "Plus ancien (id asc)" -> Comparator.comparingInt(AuthUser::id);
            default -> Comparator.comparingInt(AuthUser::id).reversed();
        };

        tableUsers.getSortOrder().clear();
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
        if (tableUsers.getScene() != null) {
            alert.getDialogPane().getStylesheets().addAll(tableUsers.getScene().getStylesheets());
        }
    }
}

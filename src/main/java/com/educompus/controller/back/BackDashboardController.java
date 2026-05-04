package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.model.AuthUser;
import com.educompus.nav.Navigator;
import com.educompus.repository.AuthUserRepository;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.File;

public final class BackDashboardController {
    private static final String[] MONTHS = { "Jan", "Fev", "Mar", "Avr", "Mai", "Jun", "Jul", "Aou", "Sep", "Oct",
            "Nov", "Dec" };

    private final AuthUserRepository authUserRepository = new AuthUserRepository();

    @FXML
    private BarChart<String, Number> acquisitionChannelChart;

    @FXML
    private LineChart<String, Number> userGrowthChart;

    @FXML
    private PieChart roleDistributionChart;

    @FXML
    private ComboBox<String> periodCombo;

    @FXML
    private Label pageTitleLabel;

    @FXML
    private Label newUsersValueLabel;

    @FXML
    private Label activeUsersValueLabel;

    @FXML
    private Label retentionValueLabel;

    @FXML
    private Label supportTicketsValueLabel;

    @FXML
    private void initialize() {
        if (pageTitleLabel != null && AppState.isTeacher()) {
            pageTitleLabel.setText("Teacher Dashboard");
        }

        if (periodCombo != null) {
            periodCombo.setItems(
                    FXCollections.observableArrayList("Ce mois", "Mois dernier", "3 derniers mois", "Annee 2026"));
            periodCombo.getSelectionModel().selectFirst();
        }

        loadUserKpisFromRepository();
    }

    @FXML
    private void onQuickOpenBackUsers(ActionEvent event) {
        openInShellCenter("View/back/BackUsers.fxml");
        syncSidebarActiveState("#navUsersBtn");
    }

    @FXML
    private void onQuickCreateUser(ActionEvent event) {
        try {
            FXMLLoader loader = Navigator.loader("View/back/BackUserForm.fxml");
            Parent root = loader.load();
            BackUserFormController controller = loader.getController();
            controller.prepareCreate();
            controller.setOnSuccess(this::loadUserKpisFromRepository);

            Stage stage = new Stage();
            stage.setTitle("Ajouter un utilisateur");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            if (pageTitleLabel != null && pageTitleLabel.getScene() != null) {
                scene.getStylesheets().addAll(pageTitleLabel.getScene().getStylesheets());
            }
            stage.setScene(scene);
            stage.setMinWidth(620);
            stage.setMinHeight(500);
            stage.showAndWait();
            loadUserKpisFromRepository();
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void onQuickImportCsv(ActionEvent event) {
        if (pageTitleLabel == null || pageTitleLabel.getScene() == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selectionner le fichier CSV des utilisateurs");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));

        File file = fileChooser.showOpenDialog(pageTitleLabel.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            FXMLLoader loader = Navigator.loader("View/back/BackUserImport.fxml");
            Parent root = loader.load();
            BackUserImportController controller = loader.getController();
            controller.loadFile(file);
            controller.setOnSuccess(this::loadUserKpisFromRepository);

            Stage stage = new Stage();
            stage.setTitle("Apercu de l'importation");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            scene.getStylesheets().addAll(pageTitleLabel.getScene().getStylesheets());
            stage.setScene(scene);
            stage.setMinWidth(920);
            stage.setMinHeight(600);
            stage.showAndWait();
            loadUserKpisFromRepository();
        } catch (Exception ignored) {
        }
    }

    private void openInShellCenter(String fxmlPath) {
        if (pageTitleLabel == null || pageTitleLabel.getScene() == null) {
            return;
        }

        Node target = pageTitleLabel.getScene().lookup("#contentWrap");
        if (!(target instanceof StackPane contentWrap)) {
            return;
        }

        try {
            Parent node = Navigator.load(fxmlPath);
            node.setOpacity(0.0);
            contentWrap.getChildren().setAll(node);
            node.setOpacity(1.0);
        } catch (Exception ignored) {
        }
    }

    private void syncSidebarActiveState(String activeButtonSelector) {
        if (pageTitleLabel == null || pageTitleLabel.getScene() == null) {
            return;
        }

        List<Node> navButtons = pageTitleLabel.getScene().getRoot().lookupAll(".nav-btn").stream().toList();
        for (Node node : navButtons) {
            if (node instanceof Button button) {
                button.getStyleClass().remove("active");
            }
        }

        Node active = pageTitleLabel.getScene().lookup(activeButtonSelector);
        if (active instanceof Button button && !button.getStyleClass().contains("active")) {
            button.getStyleClass().add("active");
        }
    }

    private void loadUserKpisFromRepository() {
        try {
            List<AuthUser> users = authUserRepository.findAll();
            applyKpis(users);
            populateRoleChart(users);
            populateGrowthChart(users);
            populateDomainChart(users);
        } catch (SQLException ex) {
            applyKpis(List.of());
            populateRoleChart(List.of());
            populateGrowthChart(List.of());
            populateDomainChart(List.of());
        }
    }

    private void applyKpis(List<AuthUser> users) {
        int totalUsers = users.size();
        int admins = 0;
        int teachers = 0;
        int students = 0;

        for (AuthUser user : users) {
            if (user.admin()) {
                admins++;
            }
            if (user.teacher()) {
                teachers++;
            }
            if (!user.admin() && !user.teacher()) {
                students++;
            }
        }

        int newestUsers = Math.min(totalUsers, Math.max(0, (int) Math.ceil(totalUsers * 0.2)));
        int activeUsers = totalUsers;
        int retentionRate = totalUsers == 0 ? 0 : (students * 100) / totalUsers;
        int governanceAlerts = admins + teachers;

        if (newUsersValueLabel != null) {
            newUsersValueLabel.setText(String.valueOf(newestUsers));
        }
        if (activeUsersValueLabel != null) {
            activeUsersValueLabel.setText(String.valueOf(activeUsers));
        }
        if (retentionValueLabel != null) {
            retentionValueLabel.setText(retentionRate + "%");
        }
        if (supportTicketsValueLabel != null) {
            supportTicketsValueLabel.setText(String.valueOf(governanceAlerts));
        }
    }

    private void populateRoleChart(List<AuthUser> users) {
        int admins = 0;
        int teachers = 0;
        int students = 0;

        for (AuthUser user : users) {
            if (user.admin()) {
                admins++;
            }
            if (user.teacher()) {
                teachers++;
            }
            if (!user.admin() && !user.teacher()) {
                students++;
            }
        }

        if (roleDistributionChart != null) {
            roleDistributionChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Etudiants", students),
                    new PieChart.Data("Teachers", teachers),
                    new PieChart.Data("Admins", admins)));
        }
    }

    private void populateGrowthChart(List<AuthUser> users) {
        if (userGrowthChart == null) {
            return;
        }

        userGrowthChart.setAnimated(false);
        userGrowthChart.setLegendVisible(false);

        List<AuthUser> sorted = new ArrayList<>(users);
        sorted.sort(Comparator.comparingInt(AuthUser::id));

        XYChart.Series<String, Number> growthSeries = new XYChart.Series<>();
        int total = sorted.size();
        for (int i = 0; i < MONTHS.length; i++) {
            int cumulativeUsers = (int) Math.round(((double) (i + 1) / MONTHS.length) * total);
            growthSeries.getData().add(new XYChart.Data<>(MONTHS[i], cumulativeUsers));
        }
        userGrowthChart.getData().setAll(growthSeries);
    }

    private void populateDomainChart(List<AuthUser> users) {
        if (acquisitionChannelChart == null) {
            return;
        }

        acquisitionChannelChart.setAnimated(false);
        acquisitionChannelChart.setLegendVisible(false);

        Map<String, Integer> domainCounts = new LinkedHashMap<>();
        domainCounts.put("gmail.com", 0);
        domainCounts.put("outlook.com", 0);
        domainCounts.put("yahoo.com", 0);
        domainCounts.put("edu.tn", 0);
        domainCounts.put("Autres", 0);

        for (AuthUser user : users) {
            String email = user.email() == null ? "" : user.email().trim().toLowerCase();
            int at = email.indexOf('@');
            String domain = (at >= 0 && at < email.length() - 1) ? email.substring(at + 1) : "";

            if (domainCounts.containsKey(domain)) {
                domainCounts.put(domain, domainCounts.get(domain) + 1);
            } else {
                domainCounts.put("Autres", domainCounts.get("Autres") + 1);
            }
        }

        XYChart.Series<String, Number> domainSeries = new XYChart.Series<>();
        for (Map.Entry<String, Integer> entry : domainCounts.entrySet()) {
            domainSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        acquisitionChannelChart.getData().setAll(domainSeries);
    }
}

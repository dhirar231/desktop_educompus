package com.educompus.controller.back;

import com.educompus.app.AppState;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

public final class BackDashboardController {
    @FXML
    private BarChart<String, Number> monthlyChart;

    @FXML
    private ComboBox<String> periodCombo;

    @FXML
    private Label pageTitleLabel;

    @FXML
    private void initialize() {
        if (pageTitleLabel != null && AppState.isTeacher()) {
            pageTitleLabel.setText("Teacher Dashboard");
        }

        if (periodCombo != null) {
            periodCombo.setItems(FXCollections.observableArrayList("Ce mois", "Mois dernier", "3 derniers mois", "Année 2026"));
            periodCombo.getSelectionModel().selectFirst();
        }

        if (monthlyChart != null) {
            monthlyChart.setAnimated(false);
            monthlyChart.setLegendVisible(false);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.getData().add(new XYChart.Data<>("Jan", 1200));
            series.getData().add(new XYChart.Data<>("Feb", 980));
            series.getData().add(new XYChart.Data<>("Mar", 760));
            series.getData().add(new XYChart.Data<>("Apr", 640));
            series.getData().add(new XYChart.Data<>("May", 1400));
            monthlyChart.getData().setAll(series);
        }
    }
}

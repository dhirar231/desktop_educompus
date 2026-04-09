package com.educompus.controller.front;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;

public final class FrontDashboardController {
    @FXML
    private BarChart<String, Number> monthlyChart;

    @FXML
    private ComboBox<String> periodCombo;

    @FXML
    private void initialize() {
        if (periodCombo != null) {
            periodCombo.setItems(FXCollections.observableArrayList("Ce mois", "Mois dernier", "3 derniers mois", "Année 2026"));
            periodCombo.getSelectionModel().selectFirst();
        }

        if (monthlyChart != null) {
            monthlyChart.setAnimated(false);
            monthlyChart.setLegendVisible(false);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.getData().add(new XYChart.Data<>("Jan", 520));
            series.getData().add(new XYChart.Data<>("Feb", 410));
            series.getData().add(new XYChart.Data<>("Mar", 300));
            series.getData().add(new XYChart.Data<>("Apr", 260));
            series.getData().add(new XYChart.Data<>("May", 720));
            monthlyChart.getData().setAll(series);
        }
    }
}

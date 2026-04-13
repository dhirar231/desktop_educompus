package com.educompus.controller.back;

import com.educompus.service.ServiceStatistiques;
import com.educompus.service.ServiceStatistiques.ProduitStat;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;

import java.util.List;
import java.util.Map;

public class BackStatistiquesController {

    // ── KPI ──────────────────────────────────────────────────────────────────
    @FXML private Label kpiTotalProduits;
    @FXML private Label kpiValeurStock;
    @FXML private Label kpiRuptures;
    @FXML private Label kpiNoteMoyenne;

    // ── Charts ────────────────────────────────────────────────────────────────
    @FXML private BarChart<String, Number>  chartCategories;
    @FXML private PieChart                  chartAvis;
    @FXML private BarChart<String, Number>  chartCA;

    // ── Top 5 ─────────────────────────────────────────────────────────────────
    @FXML private TableView<ProduitStat>           tableTop5;
    @FXML private TableColumn<ProduitStat, String> colTopNom, colTopCat;
    @FXML private TableColumn<ProduitStat, Double>  colTopNote;
    @FXML private TableColumn<ProduitStat, Integer> colTopNbAvis;

    private final ServiceStatistiques service = new ServiceStatistiques();

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        configurerTableTop5();
        charger();
    }

    private void configurerTableTop5() {
        colTopNom  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nom));
        colTopCat  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().categorie));
        colTopNote .setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().noteMoyenne).asObject());
        colTopNbAvis.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().nbAvis).asObject());

        // Affichage étoiles pour la note
        colTopNote.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(String.format("%.1f  %s", item, etoiles(item)));
                setStyle("-fx-alignment: CENTER; -fx-text-fill: #f7c32e; -fx-font-weight: 700;");
            }
        });
        colTopNbAvis.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(item));
                setStyle("-fx-alignment: CENTER;");
            }
        });
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    private void charger() {
        try { chargerKPI();          } catch (Exception e) { setErreur(kpiTotalProduits, e); }
        try { chargerChartCategories(); } catch (Exception ignored) {}
        try { chargerPieAvis();      } catch (Exception ignored) {}
        try { chargerTop5();         } catch (Exception ignored) {}
        try { chargerChartCA();      } catch (Exception ignored) {}
    }

    private void chargerKPI() throws Exception {
        kpiTotalProduits.setText(String.valueOf(service.totalProduits()));
        kpiValeurStock  .setText(String.format("%.2f TND", service.valeurTotaleStock()));
        kpiRuptures     .setText(String.valueOf(service.produitsEnRupture()));
        double moy = service.noteMoyenneGlobale();
        kpiNoteMoyenne  .setText(String.format("%.1f  %s", moy, etoiles(moy)));
    }

    private void chargerChartCategories() throws Exception {
        Map<String, Integer> data = service.produitsByCategorie();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        data.forEach((cat, nb) -> serie.getData().add(new XYChart.Data<>(cat, nb)));
        chartCategories.getData().setAll(serie);
        styleBarChart(chartCategories);
    }

    private void chargerPieAvis() throws Exception {
        Map<String, Integer> data = service.avisByNote();
        chartAvis.getData().clear();
        data.forEach((label, count) -> {
            if (count > 0) chartAvis.getData().add(new PieChart.Data(label + " (" + count + ")", count));
        });
    }

    private void chargerTop5() throws Exception {
        List<ProduitStat> liste = service.top5ProduitsNotes();
        tableTop5.setItems(FXCollections.observableArrayList(liste));
    }

    private void chargerChartCA() throws Exception {
        Map<String, Double> data = service.chiffreAffairesByCategorie();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        data.forEach((cat, ca) -> serie.getData().add(new XYChart.Data<>(cat, ca)));
        chartCA.getData().setAll(serie);
        styleBarChart(chartCA);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @FXML
    private void onActualiser(ActionEvent event) { charger(); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String etoiles(double note) {
        int plein = (int) Math.round(note);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= plein ? "★" : "☆");
        return sb.toString();
    }

    private void styleBarChart(BarChart<?, ?> chart) {
        chart.setStyle("-fx-background-color: transparent;");
        chart.lookup(".chart-plot-background")
             .setStyle("-fx-background-color: transparent;");
    }

    private void setErreur(Label lbl, Exception e) {
        lbl.setText("Err");
        lbl.setStyle("-fx-text-fill: #e74c3c;");
    }
}

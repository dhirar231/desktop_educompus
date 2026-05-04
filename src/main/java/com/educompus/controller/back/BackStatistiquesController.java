package com.educompus.controller.back;

import com.educompus.service.ServiceStatistiques;
import com.educompus.service.ServiceStatistiques.ProduitStat;
import com.educompus.service.ServiceStatistiques.ProduitVendu;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

public class BackStatistiquesController {

    // ── KPI ──────────────────────────────────────────────────────────────────
    @FXML private VBox  rootPane;
    @FXML private Label kpiTotalProduits;
    @FXML private Label kpiValeurStock;
    @FXML private Label kpiRuptures;
    @FXML private Label kpiNoteMoyenne;

    // ── Charts ────────────────────────────────────────────────────────────────
    @FXML private BarChart<String, Number>  chartCategories;
    @FXML private PieChart                  chartAvis;
    @FXML private BarChart<String, Number>  chartCA;

    // ── Top 5 notés ───────────────────────────────────────────────────────────
    @FXML private TableView<ProduitStat>           tableTop5;
    @FXML private TableColumn<ProduitStat, String> colTopNom, colTopCat;
    @FXML private TableColumn<ProduitStat, Double>  colTopNote;
    @FXML private TableColumn<ProduitStat, Integer> colTopNbAvis;

    // ── Top 5 vendus ──────────────────────────────────────────────────────────
    @FXML private TableView<ProduitVendu>            tableTop5Vendus;
    @FXML private TableColumn<ProduitVendu, String>  colVenduNom, colVenduCat;
    @FXML private TableColumn<ProduitVendu, Integer> colVenduNb;
    @FXML private TableColumn<ProduitVendu, Double>  colVenduCa;

    private final ServiceStatistiques service = new ServiceStatistiques();

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        configurerTableTop5();
        configurerTableTop5Vendus();
        charger();
    }

    private void configurerTableTop5Vendus() {
        colVenduNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nom));
        colVenduCat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().categorie));
        colVenduNb .setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().nbVentes).asObject());
        colVenduCa .setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().caTotal).asObject());

        colVenduNb.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item + " vente" + (item > 1 ? "s" : ""));
                setStyle("-fx-alignment: CENTER; -fx-font-weight: 700; -fx-text-fill: #0cbc87;");
            }
        });
        colVenduCa.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
                setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: 700; -fx-text-fill: -edu-primary;");
            }
        });
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
        try { chargerKPI();             } catch (Exception e) { setErreur(kpiTotalProduits, e); }
        try { chargerChartCategories(); } catch (Exception ignored) {}
        try { chargerPieAvis();         } catch (Exception ignored) {}
        try { chargerTop5();            } catch (Exception ignored) {}
        try { chargerTop5Vendus();      } catch (Exception ignored) {}
        try { chargerChartCA();         } catch (Exception ignored) {}
    }

    private void chargerTop5Vendus() throws Exception {
        List<ProduitVendu> liste = service.top5ProduitsVendus();
        tableTop5Vendus.setItems(FXCollections.observableArrayList(liste));
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

    @FXML
    private void onExporterPng(ActionEvent event) {
        if (rootPane == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer les statistiques en PNG");
        fc.setInitialFileName("Statistiques_Marketplace.png");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image PNG", "*.png"));
        File dest = fc.showSaveDialog(rootPane.getScene().getWindow());
        if (dest == null) return;

        new Thread(() -> {
            try {
                // Snapshot du panneau entier
                javafx.application.Platform.runLater(() -> {
                    try {
                        WritableImage img = rootPane.snapshot(new SnapshotParameters(), null);

                        // Conversion WritableImage → BufferedImage sans SwingFXUtils
                        int w = (int) img.getWidth();
                        int h = (int) img.getHeight();
                        BufferedImage bImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                        javafx.scene.image.PixelReader pr = img.getPixelReader();
                        for (int y = 0; y < h; y++) {
                            for (int x = 0; x < w; x++) {
                                bImg.setRGB(x, y, pr.getArgb(x, y));
                            }
                        }
                        ImageIO.write(bImg, "PNG", dest);

                        Alert ok = new Alert(Alert.AlertType.INFORMATION);
                        ok.setTitle("Export réussi");
                        ok.setHeaderText(null);
                        ok.setContentText("Statistiques exportées :\n" + dest.getAbsolutePath());
                        if (rootPane.getScene() != null)
                            ok.getDialogPane().getStylesheets().addAll(rootPane.getScene().getStylesheets());
                        ok.showAndWait();

                        // Ouvrir avec la visionneuse par défaut
                        try { java.awt.Desktop.getDesktop().open(dest); } catch (Exception ignored) {}
                    } catch (Exception ex) {
                        Alert err = new Alert(Alert.AlertType.ERROR);
                        err.setTitle("Erreur export");
                        err.setHeaderText(null);
                        err.setContentText("Impossible d'exporter : " + ex.getMessage());
                        err.showAndWait();
                    }
                });
            } catch (Exception ignored) {}
        }, "export-stats-png").start();
    }

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

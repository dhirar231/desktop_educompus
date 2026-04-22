package com.educompus.controller.back;

import com.educompus.model.Produit;
import com.educompus.nav.Navigator;
import com.educompus.service.ServiceProduit;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public class BackMarketplaceController {

    @FXML private ListView<Produit>  listeProduits;
    @FXML private TextField          searchField;
    @FXML private ComboBox<String>   filterCategorie;
    @FXML private ComboBox<String>   sortCombo;
    @FXML private Label              lblCount;

    private final ServiceProduit service = new ServiceProduit();
    private final ObservableList<Produit> data     = FXCollections.observableArrayList();
    private FilteredList<Produit>         filtered;
    private SortedList<Produit>           sorted;

    @FXML
    private void initialize() {
        sortCombo.getItems().addAll(
                "Nom A → Z", "Nom Z → A",
                "Prix croissant", "Prix décroissant",
                "Stock croissant", "Stock décroissant"
        );

        filtered = new FilteredList<>(data, p -> true);
        sorted   = new SortedList<>(filtered);
        listeProduits.setItems(sorted);
        listeProduits.setCellFactory(lv -> new ProduitCell());
        chargerDonnees();
    }

    // ── CellFactory ───────────────────────────────────────────────────────────

    private class ProduitCell extends ListCell<Produit> {
        private final HBox  root    = new HBox(14);
        private final VBox  infos   = new VBox(4);
        private final Label lblNom  = new Label();
        private final Label lblMeta = new Label();
        private final Label lblPrix = new Label();
        private final Label lblStock= new Label();
        private final Button btnModif = new Button("✏  Modifier");
        private final Button btnSuppr = new Button("🗑");

        ProduitCell() {
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(10, 16, 10, 16));
            root.setStyle("-fx-border-color: transparent transparent -edu-border transparent;" +
                          "-fx-border-width: 0 0 1 0;");

            lblNom.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: -edu-text;");
            lblMeta.getStyleClass().add("page-subtitle");
            lblMeta.setStyle("-fx-font-size: 11px;");

            infos.getChildren().addAll(lblNom, lblMeta);
            HBox.setHgrow(infos, Priority.ALWAYS);

            lblPrix.setStyle("-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: -edu-primary; -fx-min-width: 100px; -fx-alignment: CENTER-RIGHT;");
            lblStock.setStyle("-fx-font-size: 11px; -fx-min-width: 80px; -fx-alignment: CENTER;");

            btnModif.getStyleClass().add("btn-ghost");
            btnSuppr.getStyleClass().add("btn-ghost");
            btnSuppr.setStyle("-fx-text-fill: #e74c3c;");

            btnModif.setOnAction(e -> ouvrirModification(getItem()));
            btnSuppr.setOnAction(e -> confirmerSuppression(getItem()));

            HBox actions = new HBox(6, btnModif, btnSuppr);
            actions.setAlignment(Pos.CENTER_RIGHT);

            root.getChildren().addAll(infos, lblPrix, lblStock, actions);
        }

        @Override
        protected void updateItem(Produit p, boolean empty) {
            super.updateItem(p, empty);
            if (empty || p == null) {
                setGraphic(null);
                return;
            }
            lblNom.setText(p.getNom());
            lblMeta.setText(p.getCategorie() + "  ·  " + p.getType());
            lblPrix.setText(String.format("%.2f TND", p.getPrix()));

            if (p.getStock() == 0) {
                lblStock.setText("Rupture");
                lblStock.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c; -fx-font-weight: 700; -fx-min-width: 80px; -fx-alignment: CENTER;");
            } else if (p.getStock() <= 5) {
                lblStock.setText("Stock : " + p.getStock());
                lblStock.setStyle("-fx-font-size: 11px; -fx-text-fill: #b8860b; -fx-font-weight: 700; -fx-min-width: 80px; -fx-alignment: CENTER;");
            } else {
                lblStock.setText("Stock : " + p.getStock());
                lblStock.setStyle("-fx-font-size: 11px; -fx-text-fill: -edu-text-muted; -fx-min-width: 80px; -fx-alignment: CENTER;");
            }
            setGraphic(root);
        }
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    private void chargerDonnees() {
        try {
            data.setAll(service.afficherAll());
            filterCategorie.getItems().clear();
            data.stream().map(Produit::getCategorie).distinct().sorted()
                    .forEach(filterCategorie.getItems()::add);
            appliquerFiltre();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les produits :\n" + e.getMessage());
        }
    }

    @FXML private void onSearch() { appliquerFiltre(); }

    @FXML
    private void onSort() {
        String choix = sortCombo.getValue();
        if (choix == null) { sorted.setComparator(null); return; }
        sorted.setComparator(switch (choix) {
            case "Nom A → Z"        -> java.util.Comparator.comparing(p -> p.getNom().toLowerCase());
            case "Nom Z → A"        -> java.util.Comparator.<Produit, String>comparing(p -> p.getNom().toLowerCase()).reversed();
            case "Prix croissant"   -> java.util.Comparator.comparingDouble(Produit::getPrix);
            case "Prix décroissant" -> java.util.Comparator.comparingDouble(Produit::getPrix).reversed();
            case "Stock croissant"  -> java.util.Comparator.comparingInt(Produit::getStock);
            case "Stock décroissant"-> java.util.Comparator.comparingInt(Produit::getStock).reversed();
            default -> null;
        });
    }

    @FXML
    private void onReset() {
        searchField.clear();
        filterCategorie.getSelectionModel().clearSelection();
        sortCombo.getSelectionModel().clearSelection();
        sorted.setComparator(null);
        appliquerFiltre();
    }

    private void appliquerFiltre() {
        String txt = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String cat = filterCategorie.getValue();
        filtered.setPredicate(p -> {
            boolean matchTxt = txt.isEmpty()
                    || p.getNom().toLowerCase().contains(txt)
                    || p.getType().toLowerCase().contains(txt)
                    || p.getCategorie().toLowerCase().contains(txt);
            boolean matchCat = cat == null || cat.isBlank() || p.getCategorie().equals(cat);
            return matchTxt && matchCat;
        });
        lblCount.setText(filtered.size() + " produit(s)");
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML private void onVoirStatistiques(ActionEvent e) { naviguerVers("View/back/BackStatistiques.fxml"); }
    @FXML private void onVoirCommandes(ActionEvent e)    { naviguerVers("View/back/BackCommandes.fxml"); }

    private void naviguerVers(String fxmlPath) {
        try {
            FXMLLoader loader = Navigator.loader(fxmlPath);
            Node vue = loader.load();
            StackPane container = getContentWrap();
            if (container == null) return;
            Node marketplaceView = container.getChildren().get(0);

            VBox wrapper = new VBox();
            VBox.setVgrow(vue, Priority.ALWAYS);
            Button btnRetour = new Button("← Retour au catalogue");
            btnRetour.getStyleClass().add("btn-ghost");
            btnRetour.setOnAction(ev -> container.getChildren().setAll(marketplaceView));
            btnRetour.setStyle("-fx-padding: 10 16 10 16;");
            HBox topBar = new HBox(btnRetour);
            topBar.setStyle("-fx-background-color: -edu-card;" +
                    "-fx-border-color: transparent transparent -edu-border transparent;" +
                    "-fx-border-width: 0 0 1 0; -fx-padding: 8 18 8 18;");
            wrapper.getChildren().addAll(topBar, vue);
            wrapper.setStyle("-fx-background-color: -edu-surface-2;");
            container.getChildren().setAll(wrapper);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }

    private StackPane getContentWrap() {
        try {
            Node node = listeProduits;
            while (node.getParent() != null) {
                node = node.getParent();
                if (node instanceof StackPane sp && "contentWrap".equals(sp.getId())) return sp;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @FXML
    private void onAjouter(ActionEvent event) {
        try {
            FXMLLoader loader = Navigator.loader("View/back/BackMarketplaceAjout.fxml");
            Parent root = loader.load();
            BackMarketplaceAjoutController ctrl = loader.getController();
            ctrl.setOnSuccess(this::chargerDonnees);
            ouvrirStage("Ajouter un produit", root);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void ouvrirModification(Produit produit) {
        if (produit == null) return;
        try {
            FXMLLoader loader = Navigator.loader("View/back/BackMarketplaceModif.fxml");
            Parent root = loader.load();
            BackMarketplaceModifController ctrl = loader.getController();
            ctrl.setProduit(produit);
            ctrl.setOnSuccess(this::chargerDonnees);
            ouvrirStage("Modifier le produit", root);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void confirmerSuppression(Produit produit) {
        if (produit == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer « " + produit.getNom() + " » ?");
        confirm.setContentText("Cette action est irréversible.");
        styleAlert(confirm);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.delete(produit.getId());
                chargerDonnees();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression échouée :\n" + e.getMessage());
            }
        }
    }

    private void ouvrirStage(String titre, Parent root) {
        Stage stage = new Stage();
        stage.setTitle(titre);
        stage.initModality(Modality.APPLICATION_MODAL);
        Scene scene = new Scene(root);
        if (listeProduits.getScene() != null)
            scene.getStylesheets().addAll(listeProduits.getScene().getStylesheets());
        stage.setScene(scene);
        stage.setMinWidth(700);
        stage.setMinHeight(580);
        stage.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        styleAlert(a); a.showAndWait();
    }

    private void styleAlert(Alert a) {
        if (listeProduits.getScene() != null)
            a.getDialogPane().getStylesheets().addAll(listeProduits.getScene().getStylesheets());
    }
}

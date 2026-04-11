package com.educompus.controller.back;

import com.educompus.model.Produit;
import com.educompus.nav.Navigator;
import com.educompus.service.ServiceProduit;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public class BackMarketplaceController {

    @FXML private TableView<Produit>          tableProduits;
    @FXML private TableColumn<Produit,String> colId, colNom, colCategorie, colType, colPrix, colStock, colActions;
    @FXML private TextField                   searchField;
    @FXML private ComboBox<String>            filterCategorie;
    @FXML private Label                       lblCount;

    private final ServiceProduit service = new ServiceProduit();
    private final ObservableList<Produit> data = FXCollections.observableArrayList();
    private FilteredList<Produit> filtered;

    @FXML
    private void initialize() {
        // Colonnes texte
        colId        .setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colNom       .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colCategorie .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategorie()));
        colType      .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        colPrix      .setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getPrix())));
        colStock     .setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getStock())));

        // Colonne actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnModif = new Button("✏ Modifier");
            private final Button btnSuppr = new Button("🗑 Supprimer");
            private final HBox box = new HBox(6, btnModif, btnSuppr);

            {
                box.setAlignment(Pos.CENTER);
                btnModif.getStyleClass().add("btn-ghost");
                btnSuppr.getStyleClass().addAll("btn-ghost");
                btnSuppr.setStyle("-fx-text-fill: #e74c3c;");

                btnModif.setOnAction(e -> {
                    Produit p = getTableView().getItems().get(getIndex());
                    ouvrirModification(p);
                });
                btnSuppr.setOnAction(e -> {
                    Produit p = getTableView().getItems().get(getIndex());
                    confirmerSuppression(p);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Filtre
        filtered = new FilteredList<>(data, p -> true);
        tableProduits.setItems(filtered);

        chargerDonnees();
    }

    private void chargerDonnees() {
        try {
            data.setAll(service.afficherAll());

            // Remplir le filtre catégorie
            filterCategorie.getItems().clear();
            data.stream()
                .map(Produit::getCategorie)
                .distinct()
                .sorted()
                .forEach(filterCategorie.getItems()::add);

            appliquerFiltre();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les produits :\n" + e.getMessage());
        }
    }

    @FXML
    private void onSearch() { appliquerFiltre(); }

    @FXML
    private void onReset() {
        searchField.clear();
        filterCategorie.getSelectionModel().clearSelection();
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

    @FXML
    private void onVoirCommandes(ActionEvent event) {
        try {
            FXMLLoader loader = Navigator.loader("View/back/BackCommandes.fxml");
            Node commandesView = loader.load();

            StackPane container = getContentWrap();
            if (container == null) return;

            Node marketplaceView = container.getChildren().get(0);

            // Wrapper avec bouton retour
            javafx.scene.layout.VBox wrapper = new javafx.scene.layout.VBox();
            javafx.scene.layout.VBox.setVgrow(commandesView, javafx.scene.layout.Priority.ALWAYS);

            Button btnRetour = new Button("← Retour au catalogue");
            btnRetour.getStyleClass().add("btn-ghost");
            btnRetour.setOnAction(e -> container.getChildren().setAll(marketplaceView));
            btnRetour.setStyle("-fx-padding: 10 16 10 16;");

            HBox topBar = new HBox(btnRetour);
            topBar.setStyle("-fx-background-color: -edu-card;" +
                    "-fx-border-color: transparent transparent -edu-border transparent;" +
                    "-fx-border-width: 0 0 1 0; -fx-padding: 8 18 8 18;");

            wrapper.getChildren().addAll(topBar, commandesView);
            wrapper.setStyle("-fx-background-color: -edu-surface-2;");

            container.getChildren().setAll(wrapper);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private StackPane getContentWrap() {
        try {
            Node node = tableProduits;
            while (node.getParent() != null) {
                node = node.getParent();
                if (node instanceof StackPane sp && "contentWrap".equals(sp.getId())) return sp;
            }
        } catch (Exception ignored) {}
        return null;
    }

    @FXML
    private void onAjouter(ActionEvent event) {
        try {
            FXMLLoader loader = Navigator.loader("View/back/BackMarketplaceAjout.fxml");
            Parent root = loader.load();
            BackMarketplaceAjoutController ctrl = loader.getController();
            ctrl.setOnSuccess(this::chargerDonnees);

            Stage stage = new Stage();
            stage.setTitle("Ajouter un produit");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            // Hériter le CSS de la fenêtre principale
            if (tableProduits.getScene() != null) {
                scene.getStylesheets().addAll(tableProduits.getScene().getStylesheets());
            }
            stage.setScene(scene);
            stage.setMinWidth(700);
            stage.setMinHeight(580);
            stage.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void ouvrirModification(Produit produit) {
        try {
            FXMLLoader loader = Navigator.loader("View/back/BackMarketplaceModif.fxml");
            Parent root = loader.load();
            BackMarketplaceModifController ctrl = loader.getController();
            ctrl.setProduit(produit);
            ctrl.setOnSuccess(this::chargerDonnees);

            Stage stage = new Stage();
            stage.setTitle("Modifier le produit");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            if (tableProduits.getScene() != null) {
                scene.getStylesheets().addAll(tableProduits.getScene().getStylesheets());
            }
            stage.setScene(scene);
            stage.setMinWidth(700);
            stage.setMinHeight(580);
            stage.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void confirmerSuppression(Produit produit) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer « " + produit.getNom() + " » ?");
        confirm.setContentText("Cette action est irréversible. Voulez-vous continuer ?");
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

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        styleAlert(a);
        a.showAndWait();
    }

    private void styleAlert(Alert a) {
        if (tableProduits.getScene() != null) {
            a.getDialogPane().getStylesheets().addAll(tableProduits.getScene().getStylesheets());
        }
    }
}

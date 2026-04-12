package com.educompus.controller.front;

import com.educompus.app.AppState;
import com.educompus.model.Panier;
import com.educompus.model.Produit;
import com.educompus.nav.Navigator;
import com.educompus.service.ServicePanier;
import com.educompus.service.ServiceProduit;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FrontPanierController {

    @FXML private VBox  panierListBox;
    @FXML private VBox  panierVide;
    @FXML private VBox  footerBox;
    @FXML private Label lblTotal;

    private final ServicePanier  servicePanier  = new ServicePanier();
    private final ServiceProduit serviceProduit = new ServiceProduit();

    // Articles enrichis (Panier + Produit associé)
    private final List<PanierItem> items = new ArrayList<>();

    private Runnable onContinuerCallback;
    private Runnable onCommanderCallback; // reçoit les items pour la page commande

    public void setOnContinuer(Runnable cb) { this.onContinuerCallback = cb; }
    public void setOnCommander(Runnable cb) { this.onCommanderCallback = cb; }

    /** Retourne les articles courants pour les passer à FrontCommandeController */
    public List<PanierItem> getItems() { return items; }

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        chargerPanier();
    }

    private void chargerPanier() {
        items.clear();
        panierListBox.getChildren().clear();

        try {
            List<Panier> paniers = servicePanier.afficherByUser(AppState.getUserId());
            for (Panier p : paniers) {
                Produit prod = serviceProduit.findById(p.getProduitId());
                if (prod != null) items.add(new PanierItem(p, prod));
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }

        boolean vide = items.isEmpty();
        panierVide.setVisible(vide);
        panierVide.setManaged(vide);
        footerBox.setVisible(!vide);
        footerBox.setManaged(!vide);

        if (!vide) {
            for (PanierItem item : items) {
                panierListBox.getChildren().add(buildLigne(item));
            }
            recalculerTotal();
        }
    }

    // ── Construction d'une ligne article ─────────────────────────────────────

    private HBox buildLigne(PanierItem item) {
        HBox ligne = new HBox(14);
        ligne.getStyleClass().add("card");
        ligne.setPadding(new Insets(12, 16, 12, 16));
        ligne.setAlignment(Pos.CENTER_LEFT);

        // Nom + catégorie
        VBox infos = new VBox(4);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Label nom = new Label(item.produit.getNom());
        nom.getStyleClass().add("produit-card-title");
        nom.setWrapText(true);

        Label cat = new Label(item.produit.getCategorie() + " · " + item.produit.getType());
        cat.getStyleClass().add("produit-card-type");

        Label prixUnit = new Label(String.format("%.2f TND / unité", item.produit.getPrix()));
        prixUnit.getStyleClass().add("page-subtitle");
        prixUnit.setStyle("-fx-font-size: 11px;");

        infos.getChildren().addAll(nom, cat, prixUnit);

        // Contrôles quantité
        Button btnMoins = new Button("−");
        btnMoins.getStyleClass().add("btn-ghost");
        btnMoins.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-min-width: 32px;");

        Label lblQte = new Label(String.valueOf(item.panier.getQuantite()));
        lblQte.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-min-width: 28px; -fx-alignment: CENTER;");

        Button btnPlus = new Button("+");
        btnPlus.getStyleClass().add("btn-primary");
        btnPlus.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-min-width: 32px;");

        btnMoins.setOnAction(e -> {
            int q = item.panier.getQuantite();
            if (q > 1) {
                item.panier.setQuantite(q - 1);
                updateQuantite(item);
                lblQte.setText(String.valueOf(item.panier.getQuantite()));
                recalculerTotal();
            }
        });

        btnPlus.setOnAction(e -> {
            int q = item.panier.getQuantite();
            if (q < item.produit.getStock()) {
                item.panier.setQuantite(q + 1);
                updateQuantite(item);
                lblQte.setText(String.valueOf(item.panier.getQuantite()));
                recalculerTotal();
            }
        });

        HBox qteBox = new HBox(6, btnMoins, lblQte, btnPlus);
        qteBox.setAlignment(Pos.CENTER);

        // Sous-total
        Label sousTotal = new Label();
        sousTotal.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: -edu-primary; -fx-min-width: 90px; -fx-alignment: CENTER_RIGHT;");
        item.sousTotalLabel = sousTotal;
        rafraichirSousTotal(item);

        // Bouton supprimer
        Button btnSuppr = new Button("🗑");
        btnSuppr.getStyleClass().add("btn-ghost");
        btnSuppr.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 15px;");
        btnSuppr.setOnAction(e -> supprimerArticle(item));

        ligne.getChildren().addAll(infos, qteBox, sousTotal, btnSuppr);
        return ligne;
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private void updateQuantite(PanierItem item) {
        try {
            servicePanier.updateQuantite(item.panier);
            rafraichirSousTotal(item);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void supprimerArticle(PanierItem item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer l'article");
        confirm.setHeaderText("Retirer « " + item.produit.getNom() + " » du panier ?");
        confirm.setContentText(null);
        styleAlert(confirm);
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                servicePanier.delete(item.panier.getId());
                chargerPanier();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    @FXML
    private void onVider(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Vider le panier");
        confirm.setHeaderText("Supprimer tous les articles ?");
        confirm.setContentText("Cette action est irréversible.");
        styleAlert(confirm);
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                servicePanier.viderPanier(AppState.getUserId());
                chargerPanier();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    @FXML
    private void onContinuer(ActionEvent event) {
        if (onContinuerCallback != null) onContinuerCallback.run();
    }

    @FXML
    private void onCommander(ActionEvent event) {
        if (items.isEmpty()) return;
        try {
            FXMLLoader loader = Navigator.loader("View/front/FrontCommande.fxml");
            Node view = loader.load();
            FrontCommandeController ctrl = loader.getController();
            ctrl.setItems(items);

            StackPane container = getParentStackPane();
            if (container != null) {
                Node panierView = container.getChildren().get(0);
                ctrl.setOnRetour(() -> container.getChildren().setAll(panierView));
                ctrl.setOnSuccess(() -> {
                    // Après commande confirmée → retour catalogue
                    if (onContinuerCallback != null) onContinuerCallback.run();
                });
                container.getChildren().setAll(view);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void recalculerTotal() {
        double total = items.stream()
                .mapToDouble(i -> i.produit.getPrix() * i.panier.getQuantite())
                .sum();
        lblTotal.setText(String.format("%.2f TND", total));
    }

    private void rafraichirSousTotal(PanierItem item) {
        if (item.sousTotalLabel != null) {
            item.sousTotalLabel.setText(
                    String.format("%.2f TND", item.produit.getPrix() * item.panier.getQuantite()));
        }
    }

    private StackPane getParentStackPane() {
        try {
            Node node = panierListBox;
            while (node.getParent() != null) {
                node = node.getParent();
                if (node instanceof StackPane sp && "contentWrap".equals(sp.getId())) return sp;
            }
        } catch (Exception ignored) {}
        return null;
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
        if (panierListBox.getScene() != null)
            a.getDialogPane().getStylesheets().addAll(panierListBox.getScene().getStylesheets());
    }

    // ── Inner class ──────────────────────────────────────────────────────────

    public static class PanierItem {
        public final Panier  panier;
        public final Produit produit;
        public Label sousTotalLabel; // référence pour mise à jour live

        public PanierItem(Panier panier, Produit produit) {
            this.panier  = panier;
            this.produit = produit;
        }
    }
}

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

public class FrontMarketplaceController {

    @FXML private TextField  searchField;
    @FXML private FlowPane   cardsPane;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox       emptyState;
    @FXML private Label      lblCount;
    @FXML private Button     btnPanier;

    // Conteneur parent pour la navigation interne (liste ↔ détail)
    private StackPane parentContainer;

    private final ServiceProduit service       = new ServiceProduit();
    private final ServicePanier  servicePanier = new ServicePanier();
    private List<Produit> allProduits;
    private List<Produit> produitsFiltres;

    // Pagination
    private static final int PAGE_SIZE = 6;
    private int pageCourante = 0;

    @FXML
    private void initialize() {
        chargerProduits();
    }

    /** Appelé par le FrontShellController pour passer le StackPane central */
    public void setParentContainer(StackPane container) {
        this.parentContainer = container;
    }

    // ── Chargement ───────────────────────────────────────────────────────────

    private void chargerProduits() {
        try {
            allProduits = service.afficherAll();
            produitsFiltres = allProduits;
            pageCourante = 0;
            afficherCartes(produitsFiltres);
            mettreAJourBadgePanier();
        } catch (Exception e) {
            afficherErreur(e.getMessage());
        }
    }

    // ── Recherche ────────────────────────────────────────────────────────────

    @FXML
    private void onSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        produitsFiltres = allProduits.stream()
                .filter(p -> p.getNom().toLowerCase().contains(query))
                .collect(Collectors.toList());
        pageCourante = 0;
        afficherCartes(produitsFiltres);
    }

    @FXML
    private void onReset() {
        searchField.clear();
        produitsFiltres = allProduits;
        pageCourante = 0;
        afficherCartes(produitsFiltres);
    }

    // ── Panier ───────────────────────────────────────────────────────────────

    @FXML
    private void onVoirMesCommandes(ActionEvent event) {
        try {
            FXMLLoader loader = Navigator.loader("View/front/FrontMesCommandes.fxml");
            Node view = loader.load();
            FrontMesCommandesController ctrl = loader.getController();

            StackPane container = getParentStackPane();
            if (container == null) return;

            Node listeView = container.getChildren().get(0);
            ctrl.setOnRetour(() -> container.getChildren().setAll(listeView));
            container.getChildren().setAll(view);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void onOuvrirPanier(ActionEvent event) {        try {
            FXMLLoader loader = Navigator.loader("View/front/FrontPanier.fxml");
            Node panierView = loader.load();
            FrontPanierController ctrl = loader.getController();

            StackPane container = getParentStackPane();
            if (container == null) return;

            Node listeView = container.getChildren().get(0);

            // "Continuer mes achats" → retour à la liste
            ctrl.setOnContinuer(() -> {
                container.getChildren().setAll(listeView);
                mettreAJourBadgePanier();
            });

            container.getChildren().setAll(panierView);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void mettreAJourBadgePanier() {
        try {
            int nb = servicePanier.afficherByUser(AppState.getUserId()).size();
            btnPanier.setText("🛒  Mon panier" + (nb > 0 ? "  (" + nb + ")" : ""));
        } catch (Exception ignored) {}
    }

    // ── Affichage cartes ─────────────────────────────────────────────────────

    private void afficherCartes(List<Produit> produits) {
        cardsPane.getChildren().clear();

        boolean vide = produits == null || produits.isEmpty();
        emptyState.setVisible(vide);
        emptyState.setManaged(vide);
        scrollPane.setVisible(!vide);
        scrollPane.setManaged(!vide);

        if (vide) {
            lblCount.setText("0 produit");
            return;
        }

        int total   = produits.size();
        int debut   = pageCourante * PAGE_SIZE;
        int fin     = Math.min(debut + PAGE_SIZE, total);
        int nbPages = (int) Math.ceil((double) total / PAGE_SIZE);

        lblCount.setText(total + " produit" + (total > 1 ? "s" : "")
                + "  —  page " + (pageCourante + 1) + " / " + nbPages);

        // Cartes de la page courante
        for (int i = debut; i < fin; i++) {
            cardsPane.getChildren().add(buildCard(produits.get(i)));
        }

        // Barre de pagination
        if (nbPages > 1) {
            HBox pagination = new HBox(8);
            pagination.setAlignment(Pos.CENTER);
            pagination.setPadding(new Insets(12, 0, 4, 0));
            pagination.setMaxWidth(Double.MAX_VALUE);

            Button btnPrev = new Button("← Précédent");
            btnPrev.getStyleClass().add("btn-ghost");
            btnPrev.setDisable(pageCourante == 0);
            btnPrev.setOnAction(e -> {
                pageCourante--;
                afficherCartes(produits);
                scrollPane.setVvalue(0);
            });

            // Numéros de pages
            for (int i = 0; i < nbPages; i++) {
                final int idx = i;
                Button btnPage = new Button(String.valueOf(i + 1));
                if (i == pageCourante) {
                    btnPage.getStyleClass().add("btn-primary");
                } else {
                    btnPage.getStyleClass().add("btn-ghost");
                    btnPage.setOnAction(e -> {
                        pageCourante = idx;
                        afficherCartes(produits);
                        scrollPane.setVvalue(0);
                    });
                }
                pagination.getChildren().add(btnPage);
            }

            Button btnNext = new Button("Suivant →");
            btnNext.getStyleClass().add("btn-ghost");
            btnNext.setDisable(pageCourante >= nbPages - 1);
            btnNext.setOnAction(e -> {
                pageCourante++;
                afficherCartes(produits);
                scrollPane.setVvalue(0);
            });

            pagination.getChildren().addAll(0, List.of(btnPrev));
            pagination.getChildren().add(btnNext);

            // Ajouter la pagination dans le FlowPane comme élément pleine largeur
            // On l'ajoute dans un VBox wrapper dans le ScrollPane
            cardsPane.getChildren().add(pagination);
        }
    }

    private VBox buildCard(Produit p) {
        VBox card = new VBox(0);
        card.getStyleClass().add("produit-card");
        card.setPrefWidth(230);
        card.setMaxWidth(230);

        // Clic sur la carte → page détail
        card.setOnMouseClicked(e -> ouvrirDetail(p));

        // --- Image ---
        StackPane imgWrap = new StackPane();
        imgWrap.setPrefHeight(140);
        imgWrap.setMinHeight(140);
        imgWrap.setMaxHeight(140);
        imgWrap.getStyleClass().add("produit-card-img-wrap");

        if (p.getImage() != null && !p.getImage().isBlank()) {
            try {
                ImageView iv = new ImageView(new Image(p.getImage(), 230, 140, true, true, true));
                iv.setFitWidth(230);
                iv.setFitHeight(140);
                iv.setPreserveRatio(false);
                imgWrap.getChildren().add(iv);
            } catch (Exception ignored) {
                imgWrap.getChildren().add(buildImagePlaceholder(p));
            }
        } else {
            imgWrap.getChildren().add(buildImagePlaceholder(p));
        }

        Label badge = new Label(p.getCategorie());
        badge.getStyleClass().addAll("chip", "chip-info");
        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700;");
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(8, 0, 0, 8));
        imgWrap.getChildren().add(badge);

        // Stock status badge (top-right)
        Label stockStatus = new Label();
        stockStatus.getStyleClass().add("chip");
        stockStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: 700;");
        if (p.getStock() <= 0) {
            stockStatus.setText("Rupture");
            stockStatus.getStyleClass().add("chip-stock-rupture");
        } else if (p.getStock() <= 5) {
            stockStatus.setText("Stock faible");
            stockStatus.getStyleClass().add("chip-stock-low");
        } else {
            stockStatus.setText("Disponible");
            stockStatus.getStyleClass().add("chip-stock-available");
        }
        StackPane.setAlignment(stockStatus, Pos.TOP_RIGHT);
        StackPane.setMargin(stockStatus, new Insets(8, 8, 0, 0));
        imgWrap.getChildren().add(stockStatus);

        // --- Corps ---
        VBox body = new VBox(8);
        body.setPadding(new Insets(12, 14, 14, 14));

        Label nom = new Label(p.getNom());
        nom.getStyleClass().add("produit-card-title");
        nom.setWrapText(true);
        nom.setMaxWidth(202);

        Label type = new Label(p.getType());
        type.getStyleClass().add("produit-card-type");

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: -edu-border;");

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);

        Label prix = new Label(String.format("%.2f TND", p.getPrix()));
        prix.getStyleClass().add("produit-card-prix");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label stock = new Label("Stock : " + p.getStock());
        stock.getStyleClass().add("produit-card-stock");
        if (p.getStock() == 0) {
            stock.getStyleClass().setAll("produit-card-stock-rupture");
            stock.setText("Rupture");
        }
        footer.getChildren().addAll(prix, spacer, stock);

        // Bouton panier — stoppe la propagation du clic vers la carte
        Button btnAdd = new Button("🛒  Ajouter");
        btnAdd.getStyleClass().add("btn-primary");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setDisable(p.getStock() == 0);
        btnAdd.setOnMouseClicked(e -> e.consume()); // ne pas ouvrir le détail
        btnAdd.setOnAction(e -> onAjouterPanier(p));

        body.getChildren().addAll(nom, type, sep, footer, btnAdd);
        card.getChildren().addAll(imgWrap, body);
        return card;
    }

    private Label buildImagePlaceholder(Produit p) {
        String initiale = p.getNom() != null && !p.getNom().isBlank()
                ? String.valueOf(p.getNom().charAt(0)).toUpperCase() : "?";
        Label lbl = new Label(initiale);
        lbl.getStyleClass().add("produit-card-title");
        lbl.setStyle("-fx-font-size: 42px; -fx-font-weight: 900; -fx-opacity: 0.35;");
        return lbl;
    }

    // ── Navigation vers le détail ─────────────────────────────────────────────

    private void ouvrirDetail(Produit p) {
        try {
            FXMLLoader loader = Navigator.loader("View/front/FrontProduitDetail.fxml");
            Node detailView = loader.load();
            FrontProduitDetailController ctrl = loader.getController();
            ctrl.setProduit(p);

            // Récupérer le StackPane parent (center-wrap du shell)
            StackPane container = getParentStackPane();
            if (container != null) {
                // Sauvegarder la vue liste et afficher le détail
                Node listeView = container.getChildren().get(0);
                ctrl.setOnRetour(() -> {
                    container.getChildren().setAll(listeView);
                    mettreAJourBadgePanier();
                });
                container.getChildren().setAll(detailView);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private StackPane getParentStackPane() {
        if (parentContainer != null) return parentContainer;
        // Remonter dans le scène graph pour trouver le contentWrap du shell
        try {
            Node node = searchField;
            while (node.getParent() != null) {
                node = node.getParent();
                if (node instanceof StackPane sp && sp.getId() != null
                        && sp.getId().equals("contentWrap")) {
                    return sp;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── Panier (depuis carte) ─────────────────────────────────────────────────

    private void onAjouterPanier(Produit p) {
        try {
            Panier panier = new Panier();
            panier.setUserId(AppState.getUserId());
            panier.setProduitId(p.getId());
            panier.setQuantite(1);
            servicePanier.ajouter(panier);
            mettreAJourBadgePanier();
            showInfo("Panier", "« " + p.getNom() + " » ajouté au panier.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        styleAlert(a);
        a.showAndWait();
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
        if (searchField.getScene() != null) {
            a.getDialogPane().getStylesheets().addAll(searchField.getScene().getStylesheets());
        }
    }

    private void afficherErreur(String msg) {
        cardsPane.getChildren().clear();
        Label err = new Label("Erreur de chargement : " + msg);
        err.setStyle("-fx-text-fill: -edu-danger;");
        err.setWrapText(true);
        cardsPane.getChildren().add(err);
        lblCount.setText("");
    }
}

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

    @FXML
    private TextField searchField;
    @FXML
    private FlowPane cardsPane;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox emptyState;
    @FXML
    private Label lblCount;
    @FXML
    private Button btnPanier;

    // Conteneur parent pour la navigation interne (liste ↔ détail)
    private StackPane parentContainer;

    private final ServiceProduit service = new ServiceProduit();
    private final ServicePanier servicePanier = new ServicePanier();
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
            if (container == null)
                return;

            Node listeView = container.getChildren().get(0);
            ctrl.setOnRetour(() -> container.getChildren().setAll(listeView));
            container.getChildren().setAll(view);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void onOuvrirPanier(ActionEvent event) {
        try {
            FXMLLoader loader = Navigator.loader("View/front/FrontPanier.fxml");
            Node panierView = loader.load();
            FrontPanierController ctrl = loader.getController();

            StackPane container = getParentStackPane();
            if (container == null)
                return;

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
        } catch (Exception ignored) {
        }
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

        int total = produits.size();
        int debut = pageCourante * PAGE_SIZE;
        int fin = Math.min(debut + PAGE_SIZE, total);
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
        card.setPrefWidth(260);
        card.setMaxWidth(260);
        card.setMinWidth(260);

        // Clic sur la carte → page détail (pas sur le bouton)
        card.setOnMouseClicked(e -> ouvrirDetail(p));

        // ── Image ──────────────────────────────────────────────────────────
        StackPane imgWrap = new StackPane();
        imgWrap.setPrefHeight(160);
        imgWrap.setMinHeight(160);
        imgWrap.setMaxHeight(160);
        imgWrap.getStyleClass().add("produit-card-img-wrap");

        if (p.getImage() != null && !p.getImage().isBlank()) {
            try {
                ImageView iv = new ImageView(new Image(p.getImage(), 260, 160, true, true, true));
                iv.setFitWidth(260);
                iv.setFitHeight(160);
                iv.setPreserveRatio(false);
                imgWrap.getChildren().add(iv);
            } catch (Exception ignored) {
                imgWrap.getChildren().add(buildImagePlaceholder(p));
            }
        } else {
            imgWrap.getChildren().add(buildImagePlaceholder(p));
        }

        // Badge catégorie — haut gauche
        Label badgeCat = new Label(p.getCategorie());
        badgeCat.getStyleClass().addAll("chip", "chip-info");
        badgeCat.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 700;");
        StackPane.setAlignment(badgeCat, Pos.TOP_LEFT);
        StackPane.setMargin(badgeCat, new Insets(9, 0, 0, 9));

        // Badge stock — haut droite
        Label badgeStock = buildBadgeStock(p);
        StackPane.setAlignment(badgeStock, Pos.TOP_RIGHT);
        StackPane.setMargin(badgeStock, new Insets(9, 9, 0, 0));

        imgWrap.getChildren().addAll(badgeCat, badgeStock);
      

        // ── Corps ──────────────────────────────────────────────────────────
        VBox body = new VBox(0);
        body.setPadding(new Insets(14, 16, 16, 16));
        body.setSpacing(0);

        // Nom
        Label nom = new Label(p.getNom());
        nom.getStyleClass().add("produit-card-title");
        nom.setWrapText(true);
        nom.setMaxWidth(228);
        nom.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800;");
        VBox.setMargin(nom, new Insets(0, 0, 4, 0));

        // Type chip
        Label type = new Label(p.getType());
        type.getStyleClass().addAll("chip", "chip-outline");
        type.setStyle("-fx-font-size: 10px;");
        VBox.setMargin(type, new Insets(0, 0, 10, 0));

        // Description tronquée
        String desc = p.getDescription() != null ? p.getDescription() : "";
        if (desc.length() > 72)
            desc = desc.substring(0, 72) + "…";
        Label description = new Label(desc);
        description.getStyleClass().add("produit-card-type");
        description.setWrapText(true);
        description.setMaxWidth(228);
        description.setStyle("-fx-font-size: 11px; -fx-text-fill: -edu-text-muted;");
        VBox.setMargin(description, new Insets(0, 0, 12, 0));

        // Séparateur
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: -edu-border;");
        VBox.setMargin(sep, new Insets(0, 0, 12, 0));

        // Prix + stock
        HBox prixRow = new HBox();
        prixRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(prixRow, new Insets(0, 0, 12, 0));

        Label prix = new Label(String.format("%.2f TND", p.getPrix()));
        prix.getStyleClass().add("produit-card-prix");
        prix.setStyle("-fx-font-size: 16px; -fx-font-weight: 900;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label stockLbl = new Label(p.getStock() == 0 ? "Rupture" : p.getStock() + " en stock");
        stockLbl.setStyle(p.getStock() == 0
                ? "-fx-font-size: 11px; -fx-text-fill: #e74c3c; -fx-font-weight: 700;"
                : "-fx-font-size: 11px; -fx-text-fill: -edu-text-muted;");

        prixRow.getChildren().addAll(prix, spacer, stockLbl);

        // Bouton panier pleine largeur
        Button btnAdd = new Button(p.getStock() == 0 ? "Indisponible" : "🛒  Ajouter au panier");
        btnAdd.getStyleClass().add(p.getStock() == 0 ? "btn-ghost" : "btn-primary");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setDisable(p.getStock() == 0);
        btnAdd.setOnMouseClicked(e -> e.consume());
        btnAdd.setOnAction(e -> onAjouterPanier(p));

        body.getChildren().addAll(nom, type, description, sep, prixRow, btnAdd);
        card.getChildren().addAll(imgWrap, body);
        return card;
    }

    private Label buildImagePlaceholder(Produit p) {
        String initiale = p.getNom() != null && !p.getNom().isBlank()
                ? String.valueOf(p.getNom().charAt(0)).toUpperCase()
                : "?";
        Label lbl = new Label(initiale);
        lbl.setStyle("-fx-font-size: 52px; -fx-font-weight: 900;" +
                "-fx-text-fill: -edu-primary; -fx-opacity: 0.22;");
        return lbl;
    }

    private Label buildBadgeStock(Produit p) {
        Label badge;
        if (p.getStock() == 0) {
            badge = new Label("Rupture");
            badge.setStyle("-fx-background-color: rgba(214,41,62,0.82); -fx-text-fill: white;" +
                    "-fx-background-radius: 6px; -fx-padding: 3 8 3 8;" +
                    "-fx-font-size: 10px; -fx-font-weight: 700;");
        } else if (p.getStock() <= 5) {
            badge = new Label("Stock faible");
            badge.setStyle("-fx-background-color: rgba(247,195,46,0.88); -fx-text-fill: #5a4000;" +
                    "-fx-background-radius: 6px; -fx-padding: 3 8 3 8;" +
                    "-fx-font-size: 10px; -fx-font-weight: 700;");
        } else {
            badge = new Label("✓ Disponible");
            badge.setStyle("-fx-background-color: rgba(12,188,135,0.82); -fx-text-fill: white;" +
                    "-fx-background-radius: 6px; -fx-padding: 3 8 3 8;" +
                    "-fx-font-size: 10px; -fx-font-weight: 700;");
        }
        return badge;
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
        if (parentContainer != null)
            return parentContainer;
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
        } catch (Exception ignored) {
        }
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

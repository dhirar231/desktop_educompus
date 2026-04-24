package com.educompus.controller.front;

import com.educompus.app.AppState;
import com.educompus.model.Panier;
import com.educompus.model.Produit;
import com.educompus.nav.Navigator;
import com.educompus.service.GroqRecommandationService;
import com.educompus.service.ServicePanier;
import com.educompus.service.ServiceProduit;
import com.educompus.service.ServiceStatistiques;
import com.educompus.service.TextToSpeechService;
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

    // Section recommandations IA
    @FXML private VBox  sectionReco;
    @FXML private HBox  recoPane;
    @FXML private Label lblRecoStatus;

    private StackPane parentContainer;

    private final ServiceProduit      service       = new ServiceProduit();
    private final ServicePanier       servicePanier = new ServicePanier();
    private final ServiceStatistiques serviceStats  = new ServiceStatistiques();
    private GroqRecommandationService groqService;  // lazy init

    private List<Produit> allProduits;
    private List<Produit> produitsFiltres;

    private static final int PAGE_SIZE = 6;
    private int pageCourante = 0;

    private final java.util.concurrent.atomic.AtomicBoolean ttsEnCours =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    @FXML
    private void initialize() {
        chargerProduits();
    }

    public void setParentContainer(StackPane container) {
        this.parentContainer = container;
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    private void chargerProduits() {
        try {
            allProduits = service.afficherAll();
            produitsFiltres = allProduits;
            pageCourante = 0;
            afficherCartes(produitsFiltres);
            mettreAJourBadgePanier();
            chargerRecommandations();
        } catch (Exception e) {
            afficherErreur(e.getMessage());
        }
    }

    // ── Recommandations Groq ──────────────────────────────────────────────────

    private List<Produit> produitsRecommandes = new java.util.ArrayList<>();
    private int recoPage = 0;
    private static final int RECO_PAGE_SIZE = 3;

    private void chargerRecommandations() {
        if (sectionReco == null) return;
        lblRecoStatus.setText("Analyse en cours...");
        sectionReco.setVisible(true);
        sectionReco.setManaged(true);

        new Thread(() -> {
            try {
                if (groqService == null) groqService = new GroqRecommandationService();
                System.out.println("[Groq] Appel API pour userId=" + AppState.getUserId());
                List<Produit> recommandes = groqService.recommander(AppState.getUserId(), allProduits);
                System.out.println("[Groq] " + recommandes.size() + " produits recommandes");
                javafx.application.Platform.runLater(() -> {
                    produitsRecommandes = recommandes;
                    recoPage = 0;
                    afficherRecommandations();
                });
            } catch (Exception e) {
                System.err.println("[Groq] ERREUR : " + e.getMessage());
                javafx.application.Platform.runLater(() ->
                        lblRecoStatus.setText("Erreur : " + e.getMessage()));
            }
        }, "groq-reco").start();
    }

    private void afficherRecommandations() {
        recoPane.getChildren().clear();
        if (produitsRecommandes == null || produitsRecommandes.isEmpty()) {
            lblRecoStatus.setText("Aucune recommandation");
            return;
        }

        int total   = produitsRecommandes.size();
        int debut   = recoPage * RECO_PAGE_SIZE;
        int fin     = Math.min(debut + RECO_PAGE_SIZE, total);
        int nbPages = (int) Math.ceil((double) total / RECO_PAGE_SIZE);

        lblRecoStatus.setText((recoPage + 1) + " / " + nbPages);

        // Flèche gauche
        Button btnPrev = new Button("‹");
        btnPrev.setStyle("-fx-background-color: rgba(6,106,201,0.12); -fx-text-fill: #2563eb;" +
                "-fx-background-radius: 999px; -fx-font-size: 18px; -fx-font-weight: 900;" +
                "-fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px;" +
                "-fx-cursor: hand; -fx-border-color: transparent;");
        btnPrev.setDisable(recoPage == 0);
        btnPrev.setOpacity(recoPage == 0 ? 0.3 : 1.0);
        btnPrev.setOnAction(e -> { recoPage--; afficherRecommandations(); });

        // Mini-cards côte à côte
        HBox cardsRow = new HBox(10);
        cardsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(cardsRow, Priority.ALWAYS);
        for (int i = debut; i < fin; i++) {
            cardsRow.getChildren().add(buildMiniCard(produitsRecommandes.get(i)));
        }

        // Flèche droite
        Button btnNext = new Button("›");
        btnNext.setStyle("-fx-background-color: rgba(6,106,201,0.12); -fx-text-fill: #2563eb;" +
                "-fx-background-radius: 999px; -fx-font-size: 18px; -fx-font-weight: 900;" +
                "-fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px;" +
                "-fx-cursor: hand; -fx-border-color: transparent;");
        btnNext.setDisable(recoPage >= nbPages - 1);
        btnNext.setOpacity(recoPage >= nbPages - 1 ? 0.3 : 1.0);
        btnNext.setOnAction(e -> { recoPage++; afficherRecommandations(); });

        recoPane.getChildren().addAll(btnPrev, cardsRow, btnNext);
    }

    /** Mini-card verticale compacte : image + titre + prix + bouton */
    private VBox buildMiniCard(Produit p) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(160); card.setMaxWidth(160); card.setMinWidth(160);
        card.setPadding(new Insets(10, 10, 12, 10));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14px;" +
            "-fx-border-color: rgba(6,106,201,0.18);" +
            "-fx-border-radius: 14px;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.08), 10, 0.10, 0, 3);" +
            "-fx-cursor: hand;"
        );
        card.setOnMouseClicked(e -> ouvrirDetail(p));

        // Avatar circulaire 48x48
        StackPane avatar = new StackPane();
        avatar.setMinWidth(48); avatar.setMaxWidth(48);
        avatar.setMinHeight(48); avatar.setMaxHeight(48);
        avatar.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, rgba(0,210,255,0.2), rgba(106,17,203,0.2));" +
            "-fx-background-radius: 999px;"
        );

        if (p.getImage() != null && !p.getImage().isBlank()) {
            try {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                        new javafx.scene.image.Image(p.getImage(), 48, 48, true, true, true));
                iv.setFitWidth(48); iv.setFitHeight(48);
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(24, 24, 24);
                iv.setClip(clip);
                avatar.getChildren().add(iv);
            } catch (Exception ignored) { addInitiale(avatar, p); }
        } else { addInitiale(avatar, p); }

        // Titre
        Label nom = new Label(p.getNom());
        nom.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");
        nom.setWrapText(true);
        nom.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        nom.setMaxWidth(140);

        // Prix
        Label prix = new Label(String.format("%.2f TND", p.getPrix()));
        prix.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #2563eb;");

        // Bouton
        Button btnAdd = new Button(p.getStock() == 0 ? "Indisponible" : "+ Panier");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setStyle(p.getStock() == 0
            ? "-fx-background-color: #f1f5f9; -fx-text-fill: #94a3b8; -fx-background-radius: 8px; -fx-font-size: 10px; -fx-padding: 5 8 5 8;"
            : "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 8px; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 5 8 5 8; -fx-cursor: hand;");
        btnAdd.setDisable(p.getStock() == 0);
        btnAdd.setOnMouseClicked(e -> e.consume());
        btnAdd.setOnAction(e -> onAjouterPanier(p));

        card.getChildren().addAll(avatar, nom, prix, btnAdd);
        return card;
    }

    private void addInitiale(StackPane avatar, Produit p) {
        String init = p.getNom() != null && !p.getNom().isBlank()
                ? String.valueOf(p.getNom().charAt(0)).toUpperCase() : "?";
        Label l = new Label(init);
        l.setStyle("-fx-font-size: 20px; -fx-font-weight: 900;" +
                "-fx-text-fill: linear-gradient(to right, #2563eb, #7c3aed);");
        avatar.getChildren().add(l);
    }

    // ── Recherche ─────────────────────────────────────────────────────────────

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

    // ── Navigation ────────────────────────────────────────────────────────────

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
    private void onOuvrirPanier(ActionEvent event) {
        try {
            FXMLLoader loader = Navigator.loader("View/front/FrontPanier.fxml");
            Node panierView = loader.load();
            FrontPanierController ctrl = loader.getController();
            StackPane container = getParentStackPane();
            if (container == null) return;
            Node listeView = container.getChildren().get(0);
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
            btnPanier.setText("Mon panier" + (nb > 0 ? " (" + nb + ")" : ""));
        } catch (Exception ignored) {}
    }

    // ── Affichage cartes ──────────────────────────────────────────────────────

    private void afficherCartes(List<Produit> produits) {
        cardsPane.getChildren().clear();
        boolean vide = produits == null || produits.isEmpty();
        emptyState.setVisible(vide);
        emptyState.setManaged(vide);
        scrollPane.setVisible(!vide);
        scrollPane.setManaged(!vide);

        if (vide) { lblCount.setText("0 produit"); return; }

        int total   = produits.size();
        int debut   = pageCourante * PAGE_SIZE;
        int fin     = Math.min(debut + PAGE_SIZE, total);
        int nbPages = (int) Math.ceil((double) total / PAGE_SIZE);

        lblCount.setText(total + " produit" + (total > 1 ? "s" : "") +
                "  —  page " + (pageCourante + 1) + " / " + nbPages);

        for (int i = debut; i < fin; i++) cardsPane.getChildren().add(buildCard(produits.get(i)));

        if (nbPages > 1) {
            HBox pagination = new HBox(8);
            pagination.setAlignment(Pos.CENTER);
            pagination.setPadding(new Insets(12, 0, 4, 0));
            pagination.setMaxWidth(Double.MAX_VALUE);

            Button btnPrev = new Button("Precedent");
            btnPrev.getStyleClass().add("btn-ghost");
            btnPrev.setDisable(pageCourante == 0);
            btnPrev.setOnAction(e -> { pageCourante--; afficherCartes(produits); scrollPane.setVvalue(0); });

            for (int i = 0; i < nbPages; i++) {
                final int idx = i;
                Button btnPage = new Button(String.valueOf(i + 1));
                if (i == pageCourante) {
                    btnPage.getStyleClass().add("btn-primary");
                } else {
                    btnPage.getStyleClass().add("btn-ghost");
                    btnPage.setOnAction(e -> { pageCourante = idx; afficherCartes(produits); scrollPane.setVvalue(0); });
                }
                pagination.getChildren().add(btnPage);
            }

            Button btnNext = new Button("Suivant");
            btnNext.getStyleClass().add("btn-ghost");
            btnNext.setDisable(pageCourante >= nbPages - 1);
            btnNext.setOnAction(e -> { pageCourante++; afficherCartes(produits); scrollPane.setVvalue(0); });

            pagination.getChildren().addAll(0, List.of(btnPrev));
            pagination.getChildren().add(btnNext);
            cardsPane.getChildren().add(pagination);
        }
    }

    private VBox buildCard(Produit p) {
        VBox card = new VBox(0);
        card.getStyleClass().add("produit-card");
        card.setPrefWidth(260); card.setMaxWidth(260); card.setMinWidth(260);
        card.setOnMouseClicked(e -> ouvrirDetail(p));

        StackPane imgWrap = new StackPane();
        imgWrap.setPrefHeight(160); imgWrap.setMinHeight(160); imgWrap.setMaxHeight(160);
        imgWrap.getStyleClass().add("produit-card-img-wrap");

        if (p.getImage() != null && !p.getImage().isBlank()) {
            try {
                ImageView iv = new ImageView(new Image(p.getImage(), 260, 160, true, true, true));
                iv.setFitWidth(260); iv.setFitHeight(160); iv.setPreserveRatio(false);
                imgWrap.getChildren().add(iv);
            } catch (Exception ignored) { imgWrap.getChildren().add(buildImagePlaceholder(p)); }
        } else { imgWrap.getChildren().add(buildImagePlaceholder(p)); }

        Label badgeCat = new Label(p.getCategorie());
        badgeCat.getStyleClass().addAll("chip", "chip-info");
        badgeCat.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 700;");
        StackPane.setAlignment(badgeCat, Pos.TOP_LEFT);
        StackPane.setMargin(badgeCat, new Insets(9, 0, 0, 9));

        Label badgeStock = buildBadgeStock(p);
        StackPane.setAlignment(badgeStock, Pos.TOP_RIGHT);
        StackPane.setMargin(badgeStock, new Insets(9, 9, 0, 0));
        imgWrap.getChildren().addAll(badgeCat, badgeStock);

        VBox body = new VBox(0);
        body.setPadding(new Insets(14, 16, 16, 16));

        Label nom = new Label(p.getNom());
        nom.getStyleClass().add("produit-card-title");
        nom.setWrapText(true); nom.setMaxWidth(228);
        nom.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800;");
        VBox.setMargin(nom, new Insets(0, 0, 4, 0));

        Label type = new Label(p.getType());
        type.getStyleClass().addAll("chip", "chip-outline");
        type.setStyle("-fx-font-size: 10px;");
        VBox.setMargin(type, new Insets(0, 0, 10, 0));

        String desc = p.getDescription() != null ? p.getDescription() : "";
        if (desc.length() > 72) desc = desc.substring(0, 72) + "...";
        Label description = new Label(desc);
        description.getStyleClass().add("produit-card-type");
        description.setWrapText(true); description.setMaxWidth(228);
        description.setStyle("-fx-font-size: 11px; -fx-text-fill: -edu-text-muted;");
        VBox.setMargin(description, new Insets(0, 0, 12, 0));

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: -edu-border;");
        VBox.setMargin(sep, new Insets(0, 0, 12, 0));

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

        Button btnStart = new Button("🔊");
        btnStart.getStyleClass().add("btn-ghost");
        btnStart.setStyle("-fx-font-size: 14px; -fx-min-width: 34px;");
        btnStart.setTooltip(new Tooltip("Ecouter les statistiques"));

        Button btnStop = new Button("⏹");
        btnStop.getStyleClass().add("btn-ghost");
        btnStop.setStyle("-fx-font-size: 14px; -fx-min-width: 34px; -fx-text-fill: #e74c3c;");
        btnStop.setTooltip(new Tooltip("Arreter la lecture"));
        btnStop.setDisable(true);

        btnStart.setOnMouseClicked(e -> e.consume());
        btnStop.setOnMouseClicked(e -> e.consume());
        btnStart.setOnAction(e -> lireStatsProduit(p, btnStart, btnStop));
        btnStop.setOnAction(e -> stopperVoix(btnStart, btnStop));

        Button btnAdd = new Button(p.getStock() == 0 ? "Indisponible" : "Ajouter");
        btnAdd.getStyleClass().add(p.getStock() == 0 ? "btn-ghost" : "btn-primary");
        HBox.setHgrow(btnAdd, Priority.ALWAYS);
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setDisable(p.getStock() == 0);
        btnAdd.setOnMouseClicked(e -> e.consume());
        btnAdd.setOnAction(e -> onAjouterPanier(p));

        HBox btnRow = new HBox(4, btnStart, btnStop, btnAdd);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        body.getChildren().addAll(nom, type, description, sep, prixRow, btnRow);
        card.getChildren().addAll(imgWrap, body);
        return card;
    }

    private Label buildImagePlaceholder(Produit p) {
        String initiale = p.getNom() != null && !p.getNom().isBlank()
                ? String.valueOf(p.getNom().charAt(0)).toUpperCase() : "?";
        Label lbl = new Label(initiale);
        lbl.setStyle("-fx-font-size: 52px; -fx-font-weight: 900; -fx-text-fill: -edu-primary; -fx-opacity: 0.22;");
        return lbl;
    }

    private Label buildBadgeStock(Produit p) {
        Label badge;
        if (p.getStock() == 0) {
            badge = new Label("Rupture");
            badge.setStyle("-fx-background-color: rgba(214,41,62,0.82); -fx-text-fill: white;" +
                    "-fx-background-radius: 6px; -fx-padding: 3 8 3 8; -fx-font-size: 10px; -fx-font-weight: 700;");
        } else if (p.getStock() <= 5) {
            badge = new Label("Stock faible");
            badge.setStyle("-fx-background-color: rgba(247,195,46,0.88); -fx-text-fill: #5a4000;" +
                    "-fx-background-radius: 6px; -fx-padding: 3 8 3 8; -fx-font-size: 10px; -fx-font-weight: 700;");
        } else {
            badge = new Label("Disponible");
            badge.setStyle("-fx-background-color: rgba(12,188,135,0.82); -fx-text-fill: white;" +
                    "-fx-background-radius: 6px; -fx-padding: 3 8 3 8; -fx-font-size: 10px; -fx-font-weight: 700;");
        }
        return badge;
    }

    // ── Detail produit ────────────────────────────────────────────────────────

    private void ouvrirDetail(Produit p) {
        try {
            FXMLLoader loader = Navigator.loader("View/front/FrontProduitDetail.fxml");
            Node detailView = loader.load();
            FrontProduitDetailController ctrl = loader.getController();
            ctrl.setProduit(p);
            StackPane container = getParentStackPane();
            if (container != null) {
                Node listeView = container.getChildren().get(0);
                ctrl.setOnRetour(() -> { container.getChildren().setAll(listeView); mettreAJourBadgePanier(); });
                container.getChildren().setAll(detailView);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private StackPane getParentStackPane() {
        if (parentContainer != null) return parentContainer;
        try {
            Node node = searchField;
            while (node.getParent() != null) {
                node = node.getParent();
                if (node instanceof StackPane sp && sp.getId() != null && sp.getId().equals("contentWrap"))
                    return sp;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── TTS ───────────────────────────────────────────────────────────────────

    private Thread ttsThread = null;

    private void lireStatsProduit(Produit p, Button btnStart, Button btnStop) {
        if (ttsEnCours.get()) return;
        ttsEnCours.set(true);
        btnStart.setDisable(true);
        btnStop.setDisable(false);

        ttsThread = new Thread(() -> {
            try {
                ServiceStatistiques.ProduitStatDetail stats = serviceStats.statsProduit(p.getId());
                String texte = String.format(
                    "%s. Prix : %.0f dinars. Stock : %d unites. " +
                    "Note moyenne : %.1f sur 5. %d avis clients. " +
                    "Commande %d fois. Chiffre d affaires : %.0f dinars.",
                    p.getNom(), p.getPrix(), p.getStock(),
                    stats.noteMoyenne, stats.nbAvis, stats.nbCommandes, stats.caTotal);
                TextToSpeechService.lire(texte);
            } catch (Exception ex) {
                System.err.println("[TTS] " + ex.getMessage());
            } finally {
                ttsEnCours.set(false);
                javafx.application.Platform.runLater(() -> {
                    btnStart.setDisable(false);
                    btnStop.setDisable(true);
                });
            }
        }, "tts-stats");
        ttsThread.setDaemon(true);
        ttsThread.start();
    }

    private void stopperVoix(Button btnStart, Button btnStop) {
        TextToSpeechService.arreter();  // tue le processus PowerShell
        if (ttsThread != null) ttsThread.interrupt(); // débloque waitFor()
        ttsEnCours.set(false);
        btnStart.setDisable(false);
        btnStop.setDisable(true);
    }

    // ── Panier ────────────────────────────────────────────────────────────────

    private void onAjouterPanier(Produit p) {
        try {
            Panier panier = new Panier();
            panier.setUserId(AppState.getUserId());
            panier.setProduitId(p.getId());
            panier.setQuantite(1);
            servicePanier.ajouter(panier);
            mettreAJourBadgePanier();
            showInfo("Panier", p.getNom() + " ajoute au panier.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        styleAlert(a); a.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        styleAlert(a); a.showAndWait();
    }

    private void styleAlert(Alert a) {
        if (searchField.getScene() != null)
            a.getDialogPane().getStylesheets().addAll(searchField.getScene().getStylesheets());
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

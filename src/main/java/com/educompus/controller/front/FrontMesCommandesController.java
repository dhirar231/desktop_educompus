package com.educompus.controller.front;

import com.educompus.app.AppState;
import com.educompus.model.Commande;
import com.educompus.model.LigneCommande;
import com.educompus.model.Livraison;
import com.educompus.model.Produit;
import com.educompus.service.CloudinaryService;
import com.educompus.service.FacturePNGService;
import com.educompus.service.GarantiePNGService;
import com.educompus.service.ServiceCommande;
import com.educompus.service.ServiceLigneCommande;
import com.educompus.service.ServiceLivraison;
import com.educompus.service.ServiceProduit;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FrontMesCommandesController {

    @FXML private VBox  commandesBox;
    @FXML private VBox  emptyState;
    @FXML private Label lblCount;

    private final ServiceCommande      serviceCommande      = new ServiceCommande();
    private final ServiceLigneCommande serviceLigneCommande = new ServiceLigneCommande();
    private final ServiceLivraison     serviceLivraison     = new ServiceLivraison();
    private final ServiceProduit       serviceProduit       = new ServiceProduit();

    private Runnable onRetourCallback;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void setOnRetour(Runnable cb) { this.onRetourCallback = cb; }

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        chargerCommandes();
    }

    // ── Chargement ───────────────────────────────────────────────────────────

    private void chargerCommandes() {
        commandesBox.getChildren().clear();
        try {
            List<Commande> commandes = serviceCommande.afficherByUser(AppState.getUserId());

            boolean vide = commandes == null || commandes.isEmpty();
            emptyState.setVisible(vide);
            emptyState.setManaged(vide);

            if (vide) {
                lblCount.setText("Aucune commande");
                return;
            }

            lblCount.setText(commandes.size() + " commande" + (commandes.size() > 1 ? "s" : ""));

            // Trier par date décroissante (la plus récente en premier)
            commandes.sort((a, b) -> {
                if (a.getDateCommande() == null) return 1;
                if (b.getDateCommande() == null) return -1;
                return b.getDateCommande().compareTo(a.getDateCommande());
            });

            for (Commande cmd : commandes) {
                commandesBox.getChildren().add(buildCommandeCard(cmd));
            }

        } catch (Exception e) {
            Label err = new Label("Erreur de chargement : " + e.getMessage());
            err.setStyle("-fx-text-fill: -edu-danger;");
            err.setWrapText(true);
            commandesBox.getChildren().add(err);
        }
    }

    // ── Construction d'une carte commande ────────────────────────────────────

    private VBox buildCommandeCard(Commande cmd) {
        VBox card = new VBox(0);
        card.getStyleClass().add("card");

        // ── En-tête de la carte ──
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setStyle("-fx-border-color: transparent transparent -edu-border transparent;" +
                        "-fx-border-width: 0 0 1 0;");

        // Numéro + date
        VBox titreBox = new VBox(3);
        HBox.setHgrow(titreBox, Priority.ALWAYS);

        Label lblNum = new Label("Commande #" + cmd.getId());
        lblNum.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: -edu-text;");

        Label lblDate = new Label(cmd.getDateCommande() != null
                ? "Passée le " + cmd.getDateCommande().format(FMT) : "Date inconnue");
        lblDate.getStyleClass().add("page-subtitle");
        lblDate.setStyle("-fx-font-size: 11px;");

        titreBox.getChildren().addAll(lblNum, lblDate);

        // Total
        Label lblTotal = new Label(String.format("%.2f TND", cmd.getTotal()));
        lblTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: -edu-primary;");

        // Statut livraison
        Label chipStatut = buildChipStatut(cmd.getId());

        header.getChildren().addAll(titreBox, chipStatut, lblTotal);

        // ── Corps : lignes de commande ──
        VBox corps = new VBox(0);
        corps.setPadding(new Insets(0, 16, 0, 16));

        try {
            List<LigneCommande> lignes = serviceLigneCommande.afficherByCommande(cmd.getId());
            for (LigneCommande lc : lignes) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 0, 10, 0));
                row.setStyle("-fx-border-color: transparent transparent -edu-border transparent;" +
                             "-fx-border-width: 0 0 1 0;");

                // Nom produit + quantité
                VBox infos = new VBox(2);
                HBox.setHgrow(infos, Priority.ALWAYS);

                Label nomProd = new Label(lc.getNomProduit());
                nomProd.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");

                Label qte = new Label("× " + lc.getQuantite()
                        + "   (" + String.format("%.2f TND", lc.getPrixUnitaire()) + " / unité)");
                qte.getStyleClass().add("page-subtitle");
                qte.setStyle("-fx-font-size: 11px;");

                infos.getChildren().addAll(nomProd, qte);

                // Sous-total
                Label st = new Label(String.format("%.2f TND",
                        lc.getPrixUnitaire() * lc.getQuantite()));
                st.setStyle("-fx-font-weight: 800; -fx-text-fill: -edu-primary; -fx-font-size: 13px;");

                // Bouton garantie si type = Materiel
                HBox actions = new HBox(6);
                actions.setAlignment(Pos.CENTER_RIGHT);
                if (lc.getProduitId() != null) {
                    try {
                        Produit produit = serviceProduit.findById(lc.getProduitId());
                        if (produit != null && "Materiel".equalsIgnoreCase(produit.getType())) {
                            Button btnGarantie = new Button("🛡 Garantie");
                            btnGarantie.getStyleClass().add("btn-rgb-outline");
                            btnGarantie.setStyle("-fx-font-size: 10px; -fx-text-fill: #7c3aed;");
                            btnGarantie.setOnAction(e -> genererGarantie(cmd, lc, produit, btnGarantie));
                            actions.getChildren().add(btnGarantie);
                        }
                    } catch (Exception ignored) {}
                }

                row.getChildren().addAll(infos, st, actions);
                corps.getChildren().add(row);
            }
        } catch (Exception ignored) {}

        // ── Pied : infos livraison + bouton facture ──
        VBox pied = buildPiedLivraison(cmd.getId());

        // Ajouter le bouton directement dans le pied
        HBox actionBar = new HBox();
        actionBar.setAlignment(Pos.CENTER_RIGHT);
        actionBar.setPadding(new Insets(6, 0, 0, 0));

        Button btnPng = new Button("🖼  Télécharger la facture");
        btnPng.getStyleClass().add("btn-primary");
        btnPng.setStyle("-fx-font-size: 11px;");
        btnPng.setOnAction(e -> imprimerFacturePng(cmd, btnPng));
        actionBar.getChildren().add(btnPng);
        pied.getChildren().add(actionBar);

        card.getChildren().addAll(header, corps, pied);
        return card;
    }

    private Label buildChipStatut(int cmdId) {
        String statut = "—";
        try {
            Livraison liv = serviceLivraison.findByCommande(cmdId);
            if (liv != null) statut = liv.getStatusLivraison();
        } catch (Exception ignored) {}

        Label chip = new Label(statut);
        chip.getStyleClass().add("chip");
        switch (statut) {
            case "livree"   -> chip.getStyleClass().add("chip-success");
            case "expediee" -> chip.getStyleClass().add("chip-info");
            case "annulee"  -> chip.setStyle(
                    "-fx-background-color: rgba(214,41,62,0.12); -fx-text-fill: #d6293e;" +
                    "-fx-background-radius: 999px; -fx-padding: 4 10 4 10; -fx-font-weight: 600;");
            default         -> chip.getStyleClass().add("chip-warning");
        }
        return chip;
    }

    private VBox buildPiedLivraison(int cmdId) {
        VBox pied = new VBox(4);
        pied.setPadding(new Insets(12, 16, 14, 16));
        pied.setStyle("-fx-background-color: derive(-edu-surface, -2%); " +
                      "-fx-background-radius: 0 0 14px 14px;");

        try {
            Livraison liv = serviceLivraison.findByCommande(cmdId);
            if (liv != null) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);

                Label icone = new Label("📍");
                icone.setStyle("-fx-font-size: 13px;");

                Label adresse = new Label(liv.getAdresse() + ", " + liv.getVille());
                adresse.getStyleClass().add("page-subtitle");
                adresse.setStyle("-fx-font-size: 12px;");

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                Label phone = new Label("📞 " + (liv.getPhoneNumber() != null
                        ? liv.getPhoneNumber() : "—"));
                phone.getStyleClass().add("page-subtitle");
                phone.setStyle("-fx-font-size: 12px;");

                row.getChildren().addAll(icone, adresse, sp, phone);
                pied.getChildren().add(row);

                if (liv.getDateLivraison() != null) {
                    Label dateLiv = new Label("📅 Livraison souhaitée le "
                            + liv.getDateLivraison().format(
                                    DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    dateLiv.getStyleClass().add("page-subtitle");
                    dateLiv.setStyle("-fx-font-size: 11px;");
                    pied.getChildren().add(dateLiv);
                }
            } else {
                Label noLiv = new Label("Aucune information de livraison.");
                noLiv.getStyleClass().add("page-subtitle");
                noLiv.setStyle("-fx-font-size: 11px;");
                pied.getChildren().add(noLiv);
            }
        } catch (Exception ignored) {}

        return pied;
    }

    // ── Génération garantie PNG ───────────────────────────────────────────────

    private void genererGarantie(Commande cmd, LigneCommande lc, Produit produit, Button btn) {
        btn.setDisable(true);
        btn.setText("⏳");
        new Thread(() -> {
            try {
                GarantiePNGService svc = new GarantiePNGService();
                File png = svc.generer(cmd, lc, produit);
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("🛡 Garantie");
                    try {
                        if (java.awt.Desktop.isDesktopSupported())
                            java.awt.Desktop.getDesktop().open(png);
                    } catch (Exception ignored) {}
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Certificat de garantie");
                    info.setHeaderText(null);
                    info.setContentText("Certificat généré :\n" + png.getAbsolutePath());
                    if (commandesBox.getScene() != null)
                        info.getDialogPane().getStylesheets().addAll(commandesBox.getScene().getStylesheets());
                    info.showAndWait();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("🛡 Garantie");
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Erreur");
                    err.setHeaderText(null);
                    err.setContentText("Impossible de générer la garantie :\n" + ex.getMessage());
                    if (commandesBox.getScene() != null)
                        err.getDialogPane().getStylesheets().addAll(commandesBox.getScene().getStylesheets());
                    err.showAndWait();
                });
            }
        }, "garantie-png").start();
    }

    // ── Impression facture PNG ────────────────────────────────────────────────

    private void imprimerFacturePng(Commande cmd, Button btn) {
        btn.setDisable(true);
        btn.setText("Génération…");

        new Thread(() -> {
            try {
                List<LigneCommande> lignes = serviceLigneCommande.afficherByCommande(cmd.getId());
                Livraison liv = serviceLivraison.findByCommande(cmd.getId());

                FacturePNGService service = new FacturePNGService();
                File png = service.genererPng(cmd, lignes, liv);

                // Upload vers Cloudinary
                String urlCloud = null;
                String erreurCloud = null;
                try {
                    CloudinaryService cloudinary = new CloudinaryService();
                    urlCloud = cloudinary.uploader(png, "factures");
                    System.out.println("[Cloudinary] Facture uploadée : " + urlCloud);
                } catch (Exception ex) {
                    erreurCloud = ex.getMessage();
                    System.err.println("[Cloudinary] Upload échoué : " + ex.getMessage());
                    ex.printStackTrace();
                }

                final String urlFinale   = urlCloud;
                final String errFinale   = erreurCloud;

                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("🖼  Télécharger la facture");
                    try {
                        if (Desktop.isDesktopSupported())
                            Desktop.getDesktop().open(png);
                    } catch (Exception ignored) {}

                    // Dialog avec lien cliquable
                    Dialog<Void> dialog = new Dialog<>();
                    dialog.setTitle("Facture générée");
                    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

                    VBox content = new VBox(10);
                    content.setPadding(new Insets(10));

                    Label lblLocal = new Label("📁 Fichier local :\n" + png.getAbsolutePath());
                    lblLocal.setWrapText(true);
                    lblLocal.getStyleClass().add("page-subtitle");
                    content.getChildren().add(lblLocal);

                    if (urlFinale != null) {
                        Label lblCloud = new Label("☁ Disponible en ligne :");
                        lblCloud.getStyleClass().add("stat-title");

                        Hyperlink lien = new Hyperlink(urlFinale);
                        lien.setWrapText(true);
                        lien.setMaxWidth(420);
                        lien.setStyle("-fx-text-fill: -edu-primary; -fx-font-size: 11px;");
                        lien.setOnAction(ev -> {
                            try {
                                Desktop.getDesktop().browse(new java.net.URI(urlFinale));
                            } catch (Exception ignored) {}
                        });
                        content.getChildren().addAll(lblCloud, lien);
                    } else if (errFinale != null) {
                        Label lblErr = new Label("⚠ Upload échoué : " + errFinale);
                        lblErr.getStyleClass().add("field-error");
                        lblErr.setWrapText(true);
                        content.getChildren().add(lblErr);
                    }

                    dialog.getDialogPane().setContent(content);
                    if (commandesBox.getScene() != null)
                        dialog.getDialogPane().getStylesheets().addAll(commandesBox.getScene().getStylesheets());
                    dialog.showAndWait();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("🖼  Télécharger la facture");
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Erreur");
                    err.setHeaderText(null);
                    err.setContentText("Impossible de générer la facture :\n" + ex.getMessage());
                    if (commandesBox.getScene() != null)
                        err.getDialogPane().getStylesheets().addAll(commandesBox.getScene().getStylesheets());
                    err.showAndWait();
                });
            }
        }, "facture-png").start();
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    @FXML
    private void onActualiser(ActionEvent event) { chargerCommandes(); }

    @FXML
    private void onRetour(ActionEvent event) {
        if (onRetourCallback != null) onRetourCallback.run();
    }
}

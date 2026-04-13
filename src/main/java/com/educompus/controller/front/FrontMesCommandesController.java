package com.educompus.controller.front;

import com.educompus.app.AppState;
import com.educompus.model.Commande;
import com.educompus.model.LigneCommande;
import com.educompus.model.Livraison;
import com.educompus.service.ServiceCommande;
import com.educompus.service.ServiceLigneCommande;
import com.educompus.service.ServiceLivraison;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class FrontMesCommandesController {

    @FXML private VBox  commandesBox;
    @FXML private VBox  emptyState;
    @FXML private Label lblCount;

    private final ServiceCommande      serviceCommande      = new ServiceCommande();
    private final ServiceLigneCommande serviceLigneCommande = new ServiceLigneCommande();
    private final ServiceLivraison     serviceLivraison     = new ServiceLivraison();

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

                row.getChildren().addAll(infos, st);
                corps.getChildren().add(row);
            }
        } catch (Exception ignored) {}

        // ── Pied : infos livraison ──
        VBox pied = buildPiedLivraison(cmd.getId());

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

    // ── Actions ──────────────────────────────────────────────────────────────

    @FXML
    private void onActualiser(ActionEvent event) { chargerCommandes(); }

    @FXML
    private void onRetour(ActionEvent event) {
        if (onRetourCallback != null) onRetourCallback.run();
    }
}

package com.educompus.controller.front;

import com.educompus.app.AppState;
import com.educompus.model.Commande;
import com.educompus.model.LigneCommande;
import com.educompus.model.Livraison;
import com.educompus.repository.EducompusDB;
import com.educompus.service.ServiceCommande;
import com.educompus.service.ServiceLigneCommande;
import com.educompus.service.ServiceLivraison;
import com.educompus.service.ServicePanier;
import com.educompus.service.ServiceProduit;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class FrontCommandeController {

    @FXML private VBox       recapBox;
    @FXML private Label      lblTotal;

    // Champs livraison
    @FXML private TextField  fieldAdresse;
    @FXML private TextField  fieldVille;
    @FXML private TextField  fieldPhone;
    @FXML private DatePicker fieldDateLivraison;

    // Labels d'erreur sous chaque champ
    @FXML private Label errAdresse;
    @FXML private Label errVille;
    @FXML private Label errPhone;
    @FXML private Label errDate;

    private final ServiceCommande      serviceCommande      = new ServiceCommande();
    private final ServiceLigneCommande serviceLigneCommande = new ServiceLigneCommande();
    private final ServiceLivraison     serviceLivraison     = new ServiceLivraison();
    private final ServicePanier        servicePanier        = new ServicePanier();
    private final ServiceProduit       serviceProduit       = new ServiceProduit();

    private List<FrontPanierController.PanierItem> items;
    private Runnable onRetourCallback;
    private Runnable onSuccessCallback;

    public void setOnRetour(Runnable cb)  { this.onRetourCallback  = cb; }
    public void setOnSuccess(Runnable cb) { this.onSuccessCallback = cb; }

    public void setItems(List<FrontPanierController.PanierItem> items) {
        this.items = items;
        remplirRecap();
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        attacherValidation();
    }

    // ── Validation en temps réel ──────────────────────────────────────────────

    private void attacherValidation() {

        // Adresse : obligatoire, min 5 caractères
        attachTexte(fieldAdresse, errAdresse, val -> {
            if (val.isBlank())    return "L'adresse est obligatoire.";
            if (val.length() < 5) return "L'adresse doit contenir au moins 5 caractères.";
            if (val.length() > 255) return "L'adresse ne peut pas dépasser 255 caractères.";
            return null;
        });

        // Ville : obligatoire, lettres uniquement (avec espaces/tirets), 2–120 caractères
        attachTexte(fieldVille, errVille, val -> {
            if (val.isBlank())    return "La ville est obligatoire.";
            if (val.length() < 2) return "Le nom de la ville doit contenir au moins 2 caractères.";
            if (val.length() > 120) return "La ville ne peut pas dépasser 120 caractères.";
            if (!val.matches("[\\p{L}\\s\\-']+"))
                return "La ville ne doit contenir que des lettres.";
            return null;
        });

        // Téléphone : obligatoire, format tunisien ou international
        attachTexte(fieldPhone, errPhone, val -> {
            if (val.isBlank()) return "Le numéro de téléphone est obligatoire.";
            // Accepte : +216XXXXXXXX, 216XXXXXXXX, 2XXXXXXX, 9XXXXXXX (8 chiffres TN)
            String clean = val.replaceAll("[\\s\\-\\.]", "");
            if (!clean.matches("(\\+?216)?[2-9]\\d{7}") && !clean.matches("\\+?\\d{8,15}"))
                return "Numéro invalide — ex: +21620123456 ou 20123456.";
            return null;
        });

        // Date : optionnelle, mais si renseignée doit être dans le futur (> aujourd'hui)
        fieldDateLivraison.valueProperty().addListener((obs, oldVal, newVal) ->
                validerDate(newVal));

        fieldDateLivraison.focusedProperty().addListener((obs, wasF, isF) -> {
            if (!isF) validerDate(fieldDateLivraison.getValue());
        });
    }

    private void validerDate(LocalDate val) {
        if (val == null) {
            cacher(errDate, fieldDateLivraison);
            return;
        }
        if (!val.isAfter(LocalDate.now())) {
            afficher(errDate, fieldDateLivraison,
                    "La date de livraison doit être dans le futur (à partir de demain).");
        } else {
            cacher(errDate, fieldDateLivraison);
        }
    }

    // ── Récapitulatif ────────────────────────────────────────────────────────

    private void remplirRecap() {
        recapBox.getChildren().clear();
        double total = 0;

        for (FrontPanierController.PanierItem item : items) {
            double sousTotal = item.produit.getPrix() * item.panier.getQuantite();
            total += sousTotal;

            HBox ligne = new HBox(10);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.setPadding(new Insets(8, 0, 8, 0));
            ligne.setStyle("-fx-border-color: transparent transparent -edu-border transparent;" +
                           "-fx-border-width: 0 0 1 0;");

            VBox infos = new VBox(2);
            HBox.setHgrow(infos, Priority.ALWAYS);
            Label nom = new Label(item.produit.getNom());
            nom.getStyleClass().add("produit-card-title");
            nom.setStyle("-fx-font-size: 13px;");
            Label qte = new Label("× " + item.panier.getQuantite()
                    + "   (" + String.format("%.2f TND", item.produit.getPrix()) + " / unité)");
            qte.getStyleClass().add("page-subtitle");
            qte.setStyle("-fx-font-size: 11px;");
            infos.getChildren().addAll(nom, qte);

            Label st = new Label(String.format("%.2f TND", sousTotal));
            st.setStyle("-fx-font-weight: 800; -fx-text-fill: -edu-primary;");

            ligne.getChildren().addAll(infos, st);
            recapBox.getChildren().add(ligne);
        }

        lblTotal.setText(String.format("%.2f TND", total));
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    @FXML
    private void onRetour(ActionEvent event) {
        if (onRetourCallback != null) onRetourCallback.run();
    }

    @FXML
    private void onConfirmer(ActionEvent event) {
        if (!validerTout()) return;

        // Vérifier le stock avant de commencer
        for (FrontPanierController.PanierItem item : items) {
            if (item.produit.getStock() < item.panier.getQuantite()) {
                afficher(errAdresse, fieldAdresse,
                        "Stock insuffisant pour « " + item.produit.getNom()
                        + " » (disponible : " + item.produit.getStock() + ").");
                return;
            }
        }

        java.sql.Connection conn = null;
        try {
            conn = EducompusDB.getConnection();
            conn.setAutoCommit(false); // début transaction

            double total = items.stream()
                    .mapToDouble(i -> i.produit.getPrix() * i.panier.getQuantite())
                    .sum();

            // 1. Commande
            Commande commande = new Commande();
            commande.setUserId(AppState.getUserId());
            commande.setTotal(total);
            commande.setDateCommande(LocalDateTime.now());
            serviceCommande.ajouter(commande);

            // 2. Lignes + décrémentation stock
            for (FrontPanierController.PanierItem item : items) {
                LigneCommande lc = new LigneCommande();
                lc.setCommandeId(commande.getId());
                lc.setProduitId(item.produit.getId());
                lc.setNomProduit(item.produit.getNom());
                lc.setImageProduit(item.produit.getImage());
                lc.setPrixUnitaire(item.produit.getPrix());
                lc.setQuantite(item.panier.getQuantite());
                serviceLigneCommande.ajouter(lc);

                // Décrémenter le stock — lève une exception si insuffisant
                serviceProduit.decrementeStock(item.produit.getId(), item.panier.getQuantite());
            }

            // 3. Livraison
            Livraison livraison = new Livraison();
            livraison.setCommandeId(commande.getId());
            livraison.setAdresse(fieldAdresse.getText().trim());
            livraison.setVille(fieldVille.getText().trim());
            livraison.setPhoneNumber(fieldPhone.getText().trim());
            livraison.setStatusLivraison("en_attente");
            livraison.setCreatedAt(LocalDateTime.now());
            livraison.setUpdatedAt(LocalDateTime.now());
            if (fieldDateLivraison.getValue() != null) {
                livraison.setDateLivraison(fieldDateLivraison.getValue().atStartOfDay());
            }
            serviceLivraison.ajouter(livraison);

            // 4. Vider le panier
            servicePanier.viderPanier(AppState.getUserId());

            conn.commit(); // tout réussi → on valide

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Commande confirmée");
            ok.setHeaderText("Commande #" + commande.getId() + " enregistrée !");
            ok.setContentText(
                    "Total : " + String.format("%.2f TND", total) + "\n" +
                    "Livraison à : " + livraison.getAdresse() + ", " + livraison.getVille() + "\n" +
                    "Statut : En attente de traitement."
            );
            styleAlert(ok);
            ok.showAndWait();

            if (onSuccessCallback != null) onSuccessCallback.run();

        } catch (Exception e) {
            // Rollback si n'importe quelle étape échoue
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            afficher(errAdresse, fieldAdresse, "Erreur : " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    // ── Validation complète avant soumission ──────────────────────────────────

    private boolean validerTout() {
        boolean ok = true;

        ok &= check(fieldAdresse.getText().trim(), errAdresse, fieldAdresse, val -> {
            if (val.isBlank())      return "L'adresse est obligatoire.";
            if (val.length() < 5)   return "L'adresse doit contenir au moins 5 caractères.";
            if (val.length() > 255) return "L'adresse ne peut pas dépasser 255 caractères.";
            return null;
        });

        ok &= check(fieldVille.getText().trim(), errVille, fieldVille, val -> {
            if (val.isBlank())      return "La ville est obligatoire.";
            if (val.length() < 2)   return "Le nom de la ville doit contenir au moins 2 caractères.";
            if (val.length() > 120) return "La ville ne peut pas dépasser 120 caractères.";
            if (!val.matches("[\\p{L}\\s\\-']+"))
                return "La ville ne doit contenir que des lettres.";
            return null;
        });

        ok &= check(fieldPhone.getText().trim(), errPhone, fieldPhone, val -> {
            if (val.isBlank()) return "Le numéro de téléphone est obligatoire.";
            String clean = val.replaceAll("[\\s\\-\\.]", "");
            if (!clean.matches("(\\+?216)?[2-9]\\d{7}") && !clean.matches("\\+?\\d{8,15}"))
                return "Numéro invalide — ex: +21620123456 ou 20123456.";
            return null;
        });

        // Date : optionnelle mais doit être dans le futur si renseignée
        LocalDate date = fieldDateLivraison.getValue();
        if (date != null && !date.isAfter(LocalDate.now())) {
            afficher(errDate, fieldDateLivraison,
                    "La date de livraison doit être dans le futur (à partir de demain).");
            ok = false;
        } else if (date != null) {
            cacher(errDate, fieldDateLivraison);
        }

        return ok;
    }

    // ── Helpers validation ────────────────────────────────────────────────────

    @FunctionalInterface
    private interface Regle { String valider(String val); }

    private void attachTexte(TextField field, Label err, Regle regle) {
        field.focusedProperty().addListener((obs, wasF, isF) -> {
            if (!isF) evaluer(field.getText().trim(), err, field, regle);
        });
        field.textProperty().addListener((obs, o, n) -> {
            if (err.isVisible()) evaluer(n.trim(), err, field, regle);
        });
    }

    private void evaluer(String val, Label err, javafx.scene.layout.Region field, Regle regle) {
        String msg = regle.valider(val);
        if (msg != null) afficher(err, field, msg);
        else             cacher(err, field);
    }

    private boolean check(String val, Label err, javafx.scene.layout.Region field, Regle regle) {
        String msg = regle.valider(val);
        if (msg != null) { afficher(err, field, msg); return false; }
        cacher(err, field);
        return true;
    }

    private void afficher(Label err, javafx.scene.layout.Region field, String msg) {
        err.setText(msg);
        err.setVisible(true);
        err.setManaged(true);
        err.getStyleClass().add("field-error");
        field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-border-radius: 10px;");
    }

    private void cacher(Label err, javafx.scene.layout.Region field) {
        err.setVisible(false);
        err.setManaged(false);
        err.getStyleClass().remove("field-error");
        field.setStyle("");
    }

    private void styleAlert(Alert a) {
        if (fieldAdresse.getScene() != null)
            a.getDialogPane().getStylesheets().addAll(fieldAdresse.getScene().getStylesheets());
    }
}

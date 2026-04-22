package com.educompus.controller.front;

import com.educompus.app.AppState;
import com.educompus.model.Commande;
import com.educompus.model.LigneCommande;
import com.educompus.model.Livraison;
import com.educompus.repository.CommandeRepository;
import com.educompus.repository.EducompusDB;
import com.educompus.repository.LigneCommandeRepository;
import com.educompus.repository.LivraisonRepository;
import com.educompus.repository.PanierRepository;
import com.educompus.repository.ProduitRepository;
import com.educompus.service.ServiceCommande;
import com.educompus.service.ServiceLigneCommande;
import com.educompus.service.ServiceLivraison;
import com.educompus.service.ServicePanier;
import com.educompus.service.ServiceProduit;
import com.educompus.service.StripePaymentIntent;
import com.educompus.service.StripePaymentService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import javafx.application.Platform;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    // Paiement Stripe (intégré dans la même page via WebView)
    @FXML private VBox      paymentSection;
    @FXML private Label     lblPaymentInfo;
    @FXML private Label     errPayment;
    @FXML private WebView   stripeWebView;
    @FXML private Button    btnConfirmer;
    @FXML private ScrollPane mainScrollPane;

    private final ServiceCommande      serviceCommande      = new ServiceCommande();
    private final ServiceLigneCommande serviceLigneCommande = new ServiceLigneCommande();
    private final ServiceLivraison     serviceLivraison     = new ServiceLivraison();
    private final ServicePanier        servicePanier        = new ServicePanier();
    private final ServiceProduit       serviceProduit       = new ServiceProduit();

    private final CommandeRepository commandeRepo = new CommandeRepository();
    private final LigneCommandeRepository ligneCommandeRepo = new LigneCommandeRepository();
    private final LivraisonRepository livraisonRepo = new LivraisonRepository();
    private final ProduitRepository produitRepo = new ProduitRepository();
    private final PanierRepository panierRepo = new PanierRepository();

    private StripePaymentService stripePaymentService;
    private StripePaymentIntent currentPaymentIntent;
    private boolean paymentUiShown = false;

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
        if (paymentSection != null) {
            paymentSection.setVisible(false);
            paymentSection.setManaged(false);
        }
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

        if (items == null || items.isEmpty()) {
            afficher(errAdresse, fieldAdresse, "Votre panier est vide.");
            return;
        }
        if (!verifierStockLocal()) return;

        double total = items.stream()
                .mapToDouble(i -> i.produit.getPrix() * i.panier.getQuantite())
                .sum();

        demarrerPaiementStripe(total);
    }

    private boolean verifierStockLocal() {
        for (FrontPanierController.PanierItem item : items) {
            if (item.produit.getStock() < item.panier.getQuantite()) {
                afficher(errAdresse, fieldAdresse,
                        "Stock insuffisant pour « " + item.produit.getNom()
                                + " » (disponible : " + item.produit.getStock() + ").");
                return false;
            }
        }
        return true;
    }

    private void demarrerPaiementStripe(double total) {
        // Tenter d'initialiser Stripe — si les clés manquent, finaliser directement
        try {
            if (stripePaymentService == null) stripePaymentService = new StripePaymentService();
        } catch (Exception e) {
            // Stripe non configuré → finaliser la commande directement (mode sans paiement)
            System.out.println("[INFO] Stripe non configuré (" + e.getMessage() + ") — commande directe.");
            System.out.println("[DEBUG] STRIPE_SECRET_KEY = " + System.getenv("STRIPE_SECRET_KEY"));
            System.out.println("[DEBUG] STRIPE_PUBLISHABLE_KEY = " + System.getenv("STRIPE_PUBLISHABLE_KEY"));
            finaliserSansStripe();
            return;
        }
        System.out.println("[Stripe] Service initialisé OK — création PaymentIntent...");

        long amountMinorUnits = toMinorUnits(total);
        btnConfirmer.setDisable(true);
        btnConfirmer.setText("Paiement…");

        new Thread(() -> {
            try {
                StripePaymentIntent intent = stripePaymentService.createPaymentIntent(amountMinorUnits, AppState.getUserId());
                currentPaymentIntent = intent;
                Platform.runLater(() -> afficherUiPaiement(total, intent));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnConfirmer.setDisable(false);
                    btnConfirmer.setText("✔  Confirmer la commande");
                    showErreurVisible("Erreur Stripe : " + ex.getMessage());
                });
            }
        }, "stripe-create-payment-intent").start();
    }

    private void finaliserSansStripe() {
        btnConfirmer.setDisable(true);
        btnConfirmer.setText("Enregistrement…");
        new Thread(() -> {
            try {
                Commande commande = finaliserCommandeTransaction();
                Platform.runLater(() -> {
                    btnConfirmer.setDisable(false);
                    btnConfirmer.setText("✔  Confirmer la commande");
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("Commande confirmée");
                    ok.setHeaderText("Commande #" + commande.getId() + " enregistrée !");
                    ok.setContentText("Total : " + String.format("%.2f TND", commande.getTotal())
                            + "\nStatut : En attente de traitement.");
                    styleAlert(ok);
                    ok.showAndWait();
                    if (onSuccessCallback != null) onSuccessCallback.run();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnConfirmer.setDisable(false);
                    btnConfirmer.setText("✔  Confirmer la commande");
                    showErreurVisible("Erreur : " + ex.getMessage());
                });
            }
        }, "commande-sans-stripe").start();
    }

    private void showErreurVisible(String msg) {
        // Rendre errPayment visible même s'il était managed=false
        if (errPayment != null) {
            errPayment.setText(msg);
            errPayment.setVisible(true);
            errPayment.setManaged(true);
            if (!errPayment.getStyleClass().contains("field-error"))
                errPayment.getStyleClass().add("field-error");
        } else {
            // Fallback : Alert
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur");
            a.setHeaderText(null);
            a.setContentText(msg);
            styleAlert(a);
            a.showAndWait();
        }
    }

    private void afficherUiPaiement(double total, StripePaymentIntent intent) {
        if (paymentSection != null) {
            paymentSection.setVisible(true);
            paymentSection.setManaged(true);
        }

        String currency = stripePaymentService != null ? stripePaymentService.getCurrency().toUpperCase() : "";
        if (lblPaymentInfo != null) {
            lblPaymentInfo.setText(String.format("Montant à payer : %.2f %s", total, currency));
        }

        if (stripeWebView == null) {
            showErreurVisible("WebView indisponible — javafx-web manquant ou non chargé.");
            btnConfirmer.setDisable(false);
            btnConfirmer.setText("✔  Confirmer la commande");
            return;
        }

        // Scroll automatique vers la section paiement
        Platform.runLater(() -> {
            if (mainScrollPane != null) mainScrollPane.setVvalue(1.0);
            if (paymentSection != null) paymentSection.requestFocus();
        });

        WebEngine engine = stripeWebView.getEngine();
        if (!paymentUiShown) {
            engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                if (state == Worker.State.SUCCEEDED) {
                    try {
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("javaBridge", new JavaBridge());
                        System.out.println("[Stripe] WebView chargé, javaBridge injecté.");
                    } catch (Exception e) {
                        System.err.println("[Stripe] Erreur injection javaBridge : " + e.getMessage());
                    }
                } else if (state == Worker.State.FAILED) {
                    System.err.println("[Stripe] WebView FAILED : " + engine.getLoadWorker().getException());
                    Platform.runLater(() -> showErreurVisible("Impossible de charger le formulaire de paiement. Vérifiez votre connexion internet."));
                }
            });
            paymentUiShown = true;
        }

        String html = chargerStripeHtml(stripePaymentService.getPublishableKey(), intent.clientSecret());
        engine.loadContent(html, "text/html");
        btnConfirmer.setText("Remplissez le formulaire ci-dessous →");
    }

    private String chargerStripeHtml(String publishableKey, String clientSecret) {
        String template = readResourceUtf8("/stripe/embedded-payment.html");
        return template
                .replace("__PUBLISHABLE_KEY__", escapeJsString(publishableKey))
                .replace("__CLIENT_SECRET__", escapeJsString(clientSecret));
    }

    private void onPaiementTermine(String paymentIntentId, String status) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            btnConfirmer.setDisable(false);
            btnConfirmer.setText("✔  Confirmer la commande");
            afficher(errPayment, paymentErrorRegion(), "Paiement non validé.");
            return;
        }

        btnConfirmer.setText("Validation…");

        new Thread(() -> {
            try {
                boolean succeeded = stripePaymentService != null && stripePaymentService.isSucceeded(paymentIntentId);
                if (!succeeded) {
                    Platform.runLater(() -> {
                        btnConfirmer.setDisable(false);
                        btnConfirmer.setText("✔  Confirmer la commande");
                        afficher(errPayment, paymentErrorRegion(), "Paiement non finalisé (statut: " + status + ").");
                    });
                    return;
                }

                Commande commande = finaliserCommandeTransaction();
                Platform.runLater(() -> {
                    btnConfirmer.setDisable(false);
                    btnConfirmer.setText("✔  Confirmer la commande");
                    if (paymentSection != null) {
                        paymentSection.setVisible(false);
                        paymentSection.setManaged(false);
                    }
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("Commande confirmée");
                    ok.setHeaderText("Commande #" + commande.getId() + " enregistrée !");
                    ok.setContentText("Statut : En attente de traitement.");
                    styleAlert(ok);
                    ok.showAndWait();
                    if (onSuccessCallback != null) onSuccessCallback.run();
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnConfirmer.setDisable(false);
                    btnConfirmer.setText("✔  Confirmer la commande");
                    afficher(errPayment, paymentErrorRegion(), "Erreur: " + ex.getMessage());
                });
            }
        }, "stripe-verify-and-finalize-order").start();
    }

    private Commande finaliserCommandeTransaction() throws Exception {
        java.sql.Connection conn = null;
        try {
            conn = EducompusDB.getConnection();
            conn.setAutoCommit(false);

            double total = items.stream()
                    .mapToDouble(i -> i.produit.getPrix() * i.panier.getQuantite())
                    .sum();

            Commande commande = new Commande();
            commande.setUserId(AppState.getUserId());
            commande.setTotal(total);
            commande.setDateCommande(LocalDateTime.now());
            commandeRepo.insert(conn, commande);

            for (FrontPanierController.PanierItem item : items) {
                LigneCommande lc = new LigneCommande();
                lc.setCommandeId(commande.getId());
                lc.setProduitId(item.produit.getId());
                lc.setNomProduit(item.produit.getNom());
                lc.setImageProduit(item.produit.getImage());
                lc.setPrixUnitaire(item.produit.getPrix());
                lc.setQuantite(item.panier.getQuantite());
                ligneCommandeRepo.insert(conn, lc);

                produitRepo.decrementeStock(conn, item.produit.getId(), item.panier.getQuantite());
            }

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
            livraisonRepo.insert(conn, livraison);

            panierRepo.deleteByUser(conn, AppState.getUserId());

            conn.commit();
            return commande;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    private long toMinorUnits(double amount) {
        return Math.round(amount * 100.0);
    }

    private String readResourceUtf8(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource introuvable: " + path);
            byte[] data = is.readAllBytes();
            return new String(data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private Region paymentErrorRegion() {
        return paymentSection != null ? paymentSection : fieldAdresse;
    }

    private String escapeJsString(String s) {
        if (s == null) return "";
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private class JavaBridge {
        public void onPaymentCompleted(String paymentIntentId, String status) {
            Platform.runLater(() -> onPaiementTermine(paymentIntentId, status));
        }

        public void onPaymentFailed(String message) {
            Platform.runLater(() -> afficher(errPayment, paymentErrorRegion(), message));
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

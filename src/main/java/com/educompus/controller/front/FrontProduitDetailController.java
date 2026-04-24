package com.educompus.controller.front;

import com.educompus.app.AppState;
import com.educompus.model.Avis;
import com.educompus.model.Panier;
import com.educompus.model.Produit;
import com.educompus.service.GroqRecommandationService;
import com.educompus.service.ServiceAvis;
import com.educompus.service.ServicePanier;
import com.educompus.util.BadWordsFilter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FrontProduitDetailController {

    @FXML private Label     lblTitrePage;
    @FXML private StackPane imgWrap;
    @FXML private Label     lblNom;
    @FXML private Label     lblCategorie;
    @FXML private Label     lblType;
    @FXML private Label     lblDescription;
    @FXML private Label     lblPrix;
    @FXML private Label     lblStock;
    @FXML private Button    btnAjouterPanier;

    // Avis
    @FXML private VBox      formAvisCard;
    @FXML private Label     lblFormTitre;
    @FXML private HBox      starsBox;
    @FXML private TextArea  fieldCommentaire;
    @FXML private Label     lblAvisErreur;
    @FXML private Button    btnAnnulerAvis;
    @FXML private Button    btnSoumettreAvis;
    @FXML private VBox      avisListBox;
    @FXML private VBox      avisVide;

    private final ServiceAvis   serviceAvis   = new ServiceAvis();
    private final ServicePanier servicePanier = new ServicePanier();
    private final GroqRecommandationService groqService = new GroqRecommandationService();

    // Chat
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox       chatMessages;
    @FXML private TextField  chatInput;
    private int nbConsultations = 0;

    private Produit produit;
    private Runnable onRetourCallback;

    // État étoiles
    private int noteSelectionnee = 0;
    private final List<Label> etoiles = new ArrayList<>();

    // Mode édition avis
    private Avis avisEnEdition = null;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Init ────────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        construireEtoiles();
        // Effacer la bordure rouge dès que le client modifie son commentaire
        fieldCommentaire.textProperty().addListener((obs, o, n) -> {
            if (!n.isBlank()) fieldCommentaire.setStyle("");
        });
    }

    public void setProduit(Produit p) {
        this.produit = p;
        nbConsultations++;  // incrémenter à chaque ouverture de la fiche
        remplirInfos();
        chargerAvis();
        ajouterMessageAssistant("Bonjour ! Je suis votre assistant EduCampus. Posez-moi vos questions sur \"" + p.getNom() + "\" !");
    }

    public void setOnRetour(Runnable callback) {
        this.onRetourCallback = callback;
    }

    // ── Infos produit ────────────────────────────────────────────────────────

    private void remplirInfos() {
        lblTitrePage.setText(produit.getNom());
        lblNom.setText(produit.getNom());
        lblCategorie.setText(produit.getCategorie());
        lblType.setText(produit.getType());
        lblDescription.setText(produit.getDescription());
        lblPrix.setText(String.format("%.2f TND", produit.getPrix()));

        if (produit.getStock() == 0) {
            lblStock.setText("Rupture de stock");
            lblStock.getStyleClass().setAll("produit-card-stock-rupture");
            btnAjouterPanier.setDisable(true);
        } else {
            lblStock.setText("En stock (" + produit.getStock() + ")");
            lblStock.getStyleClass().setAll("produit-card-stock");
        }

        // Image
        imgWrap.getChildren().clear();
        if (produit.getImage() != null && !produit.getImage().isBlank()) {
            try {
                ImageView iv = new ImageView(new Image(produit.getImage(), 280, 200, true, true, true));
                iv.setFitWidth(280);
                iv.setFitHeight(200);
                iv.setPreserveRatio(false);
                imgWrap.getChildren().add(iv);
            } catch (Exception ignored) {
                imgWrap.getChildren().add(buildPlaceholder());
            }
        } else {
            imgWrap.getChildren().add(buildPlaceholder());
        }
    }

    private Label buildPlaceholder() {
        String init = produit.getNom() != null && !produit.getNom().isBlank()
                ? String.valueOf(produit.getNom().charAt(0)).toUpperCase() : "?";
        Label l = new Label(init);
        l.setStyle("-fx-font-size: 64px; -fx-font-weight: 900; -fx-opacity: 0.3;");
        return l;
    }

    // ── Chat assistant ────────────────────────────────────────────────────────

    @FXML
    private void onEnvoyerMessage(ActionEvent event) {
        if (chatInput == null) return;
        String question = chatInput.getText().trim();
        if (question.isBlank()) return;

        // Afficher la question du client
        ajouterMessageClient(question);
        chatInput.clear();

        // Indicateur de frappe
        Label typing = new Label("Assistant en train d'écrire...");
        typing.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
        if (chatMessages != null) chatMessages.getChildren().add(typing);
        scrollerChat();

        String nomClient = AppState.getUserDisplayName();
        if (nomClient == null || nomClient.isBlank()) nomClient = "Etudiant";
        final String nomFinal = nomClient;

        new Thread(() -> {
            try {
                String reponse = groqService.repondreAssistant(
                        produit.getNom(), nomFinal, nbConsultations, question);
                final String rep = reponse;
                javafx.application.Platform.runLater(() -> {
                    if (chatMessages != null) chatMessages.getChildren().remove(typing);
                    ajouterMessageAssistant(rep);
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    if (chatMessages != null) chatMessages.getChildren().remove(typing);
                    ajouterMessageAssistant("Désolé, je ne peux pas répondre pour le moment. Réessayez dans un instant.");
                });
            }
        }, "groq-chat").start();
    }

    private void ajouterMessageClient(String texte) {
        if (chatMessages == null) return;
        HBox row = new HBox();
        row.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label msg = new Label(texte);
        msg.setWrapText(true);
        msg.setMaxWidth(280);
        msg.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white;" +
                "-fx-background-radius: 14px 14px 4px 14px; -fx-padding: 8 12 8 12;" +
                "-fx-font-size: 12px;");
        row.getChildren().add(msg);
        chatMessages.getChildren().add(row);
        scrollerChat();
    }

    private void ajouterMessageAssistant(String texte) {
        if (chatMessages == null) return;
        HBox row = new HBox(8);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Avatar assistant
        Label avatar = new Label("AI");
        avatar.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #2563eb);" +
                "-fx-text-fill: white; -fx-background-radius: 999px;" +
                "-fx-min-width: 28px; -fx-min-height: 28px; -fx-max-width: 28px; -fx-max-height: 28px;" +
                "-fx-alignment: CENTER; -fx-font-size: 9px; -fx-font-weight: 800;");

        Label msg = new Label(texte);
        msg.setWrapText(true);
        msg.setMaxWidth(280);
        msg.setStyle("-fx-background-color: white; -fx-text-fill: #1e293b;" +
                "-fx-background-radius: 14px 14px 14px 4px; -fx-padding: 8 12 8 12;" +
                "-fx-font-size: 12px;" +
                "-fx-border-color: rgba(6,106,201,0.15); -fx-border-radius: 14px 14px 14px 4px; -fx-border-width: 1;");

        row.getChildren().addAll(avatar, msg);
        chatMessages.getChildren().add(row);
        scrollerChat();
    }

    private void scrollerChat() {
        if (chatScrollPane != null)
            javafx.application.Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    // ── Panier ────────────────────────────────────────────────────────────────

    @FXML
    private void onAjouterPanier(ActionEvent event) {
        try {
            Panier p = new Panier();
            p.setUserId(AppState.getUserId());
            p.setProduitId(produit.getId());
            p.setQuantite(1);
            servicePanier.ajouter(p);
            showInfo("Panier", "« " + produit.getNom() + " » ajouté au panier.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ── Retour ───────────────────────────────────────────────────────────────

    @FXML
    private void onRetour(ActionEvent event) {
        if (onRetourCallback != null) {
            onRetourCallback.run();
        } else {
            // fallback : fermer la fenêtre si ouverte en Stage
            try {
                ((Stage) lblNom.getScene().getWindow()).close();
            } catch (Exception ignored) {}
        }
    }

    // ── Étoiles ──────────────────────────────────────────────────────────────

    private void construireEtoiles() {
        starsBox.getChildren().clear();
        etoiles.clear();
        for (int i = 1; i <= 5; i++) {
            final int val = i;
            Label star = new Label("☆");
            star.getStyleClass().add("star-label");
            star.setOnMouseEntered(e -> survolerEtoiles(val));
            star.setOnMouseExited(e  -> rafraichirEtoiles(noteSelectionnee));
            star.setOnMouseClicked(e -> selectionnerNote(val));
            etoiles.add(star);
            starsBox.getChildren().add(star);
        }
    }

    private void survolerEtoiles(int n) {
        for (int i = 0; i < 5; i++) {
            etoiles.get(i).setText(i < n ? "★" : "☆");
            etoiles.get(i).setStyle(i < n
                    ? "-fx-text-fill: #f7c32e; -fx-font-size: 24px; -fx-cursor: hand;"
                    : "-fx-text-fill: -edu-text-muted; -fx-font-size: 24px; -fx-cursor: hand;");
        }
    }

    private void rafraichirEtoiles(int n) {
        for (int i = 0; i < 5; i++) {
            etoiles.get(i).setText(i < n ? "★" : "☆");
            etoiles.get(i).setStyle(i < n
                    ? "-fx-text-fill: #f7c32e; -fx-font-size: 24px; -fx-cursor: hand;"
                    : "-fx-text-fill: -edu-text-muted; -fx-font-size: 24px; -fx-cursor: hand;");
        }
    }

    private void selectionnerNote(int n) {
        noteSelectionnee = n;
        rafraichirEtoiles(n);
    }

    // ── Avis CRUD ────────────────────────────────────────────────────────────

    private void chargerAvis() {
        avisListBox.getChildren().clear();
        try {
            List<Avis> liste = serviceAvis.afficherByProduit(produit.getId());
            boolean vide = liste == null || liste.isEmpty();
            avisVide.setVisible(vide);
            avisVide.setManaged(vide);
            if (!vide) {
                for (Avis a : liste) {
                    avisListBox.getChildren().add(buildAvisCard(a));
                }
            }
        } catch (Exception e) {
            Label err = new Label("Erreur chargement avis : " + e.getMessage());
            err.setStyle("-fx-text-fill: -edu-danger;");
            avisListBox.getChildren().add(err);
        }
    }

    private VBox buildAvisCard(Avis a) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        // Ligne : étoiles + date
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label stars = new Label(buildStarsString(a.getNote()));
        stars.setStyle("-fx-text-fill: #f7c32e; -fx-font-size: 16px;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label date = new Label(a.getCreatedAt() != null ? a.getCreatedAt().format(FMT) : "");
        date.getStyleClass().add("page-subtitle");
        date.setStyle("-fx-font-size: 11px;");

        header.getChildren().addAll(stars, sp, date);

        // Commentaire
        Label commentaire = new Label(a.getCommentaire() != null ? a.getCommentaire() : "");
        commentaire.setWrapText(true);
        commentaire.getStyleClass().add("page-subtitle");
        commentaire.setStyle("-fx-text-fill: -edu-text;");

        card.getChildren().addAll(header, commentaire);

        // Boutons modifier/supprimer uniquement pour l'auteur
        if (a.getUserId() == AppState.getUserId()) {
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);

            Button btnModif = new Button("✏ Modifier");
            btnModif.getStyleClass().add("btn-ghost");
            btnModif.setStyle("-fx-font-size: 11px;");
            btnModif.setOnAction(e -> entrerModeEdition(a));

            Button btnSuppr = new Button("🗑 Supprimer");
            btnSuppr.getStyleClass().add("btn-ghost");
            btnSuppr.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
            btnSuppr.setOnAction(e -> supprimerAvis(a));

            actions.getChildren().addAll(btnModif, btnSuppr);
            card.getChildren().add(actions);
        }

        return card;
    }

    private String buildStarsString(int note) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= note ? "★" : "☆");
        return sb.toString();
    }

    @FXML
    private void onSoumettreAvis(ActionEvent event) {
        cacherErreurAvis();

        if (noteSelectionnee == 0) {
            afficherErreurAvis("Veuillez sélectionner une note (1 à 5 étoiles).");
            return;
        }
        if (fieldCommentaire.getText().isBlank()) {
            afficherErreurAvis("Le commentaire ne peut pas être vide.");
            return;
        }

        // Vérification des mots interdits
        String commentaire = fieldCommentaire.getText().trim();
        if (BadWordsFilter.contientMotInterdit(commentaire)) {
            String motTrouve = BadWordsFilter.premierMotInterdit(commentaire);
            afficherErreurAvis("Votre commentaire contient un mot inapproprié"
                    + (motTrouve != null ? " (\"" + motTrouve + "\")" : "")
                    + ". Veuillez le reformuler.");
            // Mettre en évidence le champ
            fieldCommentaire.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-border-radius: 10px;");
            return;
        }
        // Réinitialiser le style si OK
        fieldCommentaire.setStyle("");

        try {
            if (avisEnEdition != null) {
                // Mode modification
                avisEnEdition.setNote(noteSelectionnee);
                avisEnEdition.setCommentaire(fieldCommentaire.getText().trim());
                serviceAvis.update(avisEnEdition);
            } else {
                // Nouvel avis
                Avis a = new Avis();
                a.setUserId(AppState.getUserId());
                a.setProduitId(produit.getId());
                a.setNote(noteSelectionnee);
                a.setCommentaire(fieldCommentaire.getText().trim());
                a.setCreatedAt(LocalDateTime.now());
                serviceAvis.ajouter(a);
            }
            reinitialiserFormulaire();
            chargerAvis();
        } catch (Exception e) {
            afficherErreurAvis("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void onAnnulerAvis(ActionEvent event) {
        reinitialiserFormulaire();
    }

    private void entrerModeEdition(Avis a) {
        avisEnEdition = a;
        lblFormTitre.setText("Modifier votre avis");
        fieldCommentaire.setText(a.getCommentaire() != null ? a.getCommentaire() : "");
        selectionnerNote(a.getNote());
        btnSoumettreAvis.setText("Enregistrer les modifications");
        btnAnnulerAvis.setVisible(true);
        btnAnnulerAvis.setManaged(true);
        // Scroll vers le formulaire
        formAvisCard.requestFocus();
    }

    private void supprimerAvis(Avis a) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer l'avis");
        confirm.setHeaderText("Supprimer votre avis ?");
        confirm.setContentText("Cette action est irréversible.");
        styleAlert(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                serviceAvis.delete(a.getId());
                chargerAvis();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    private void reinitialiserFormulaire() {
        avisEnEdition = null;
        lblFormTitre.setText("Laisser un avis");
        fieldCommentaire.clear();
        noteSelectionnee = 0;
        rafraichirEtoiles(0);
        btnSoumettreAvis.setText("Publier l'avis");
        btnAnnulerAvis.setVisible(false);
        btnAnnulerAvis.setManaged(false);
        cacherErreurAvis();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void afficherErreurAvis(String msg) {
        lblAvisErreur.setText(msg);
        lblAvisErreur.setVisible(true);
        lblAvisErreur.setManaged(true);
    }

    private void cacherErreurAvis() {
        lblAvisErreur.setVisible(false);
        lblAvisErreur.setManaged(false);
    }

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
        if (lblNom.getScene() != null) {
            a.getDialogPane().getStylesheets().addAll(lblNom.getScene().getStylesheets());
        }
    }
}

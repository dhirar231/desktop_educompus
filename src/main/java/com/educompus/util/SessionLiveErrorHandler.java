package com.educompus.util;

import com.educompus.exception.InvalidSessionLinkException;
import com.educompus.exception.SessionNotActiveException;
import com.educompus.exception.SessionNotFoundException;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.awt.Desktop;
import java.net.URI;

/**
 * Gestionnaire centralisé des erreurs du module Session Live.
 * Fournit des messages clairs et une logique de fallback (copie du lien).
 */
public final class SessionLiveErrorHandler {

    private SessionLiveErrorHandler() {}

    /**
     * Gère une exception de session et affiche un message approprié.
     * @param e L'exception à gérer
     * @param lienFallback Lien à copier en fallback (peut être null)
     */
    public static void gerer(Exception e, String lienFallback) {
        if (e instanceof SessionNotActiveException ex) {
            afficherErreur("Session non disponible",
                "Cette session n'est pas encore active.\n" +
                "Attendez que l'enseignant démarre la session.",
                lienFallback);
        } else if (e instanceof InvalidSessionLinkException ex) {
            afficherErreur("Lien invalide",
                "Le lien de cette session n'est pas valide.\n" +
                "Contactez votre enseignant pour obtenir le bon lien.\n\n" +
                "Lien reçu : " + ex.getLien(),
                lienFallback);
        } else if (e instanceof SessionNotFoundException ex) {
            afficherErreur("Session introuvable",
                "La session demandée n'existe plus.\n" +
                "Actualisez la page pour voir les sessions disponibles.",
                null);
        } else {
            afficherErreur("Erreur de connexion",
                "Impossible d'ouvrir la session.\n\n" +
                "Cause : " + (e != null ? e.getMessage() : "Erreur inconnue"),
                lienFallback);
        }
    }

    /**
     * Tente d'ouvrir un lien dans le navigateur avec fallback copie presse-papier.
     * @param lien L'URL à ouvrir
     * @return true si ouvert avec succès, false si fallback utilisé
     */
    public static boolean ouvrirLienAvecFallback(String lien) {
        if (lien == null || lien.isBlank()) {
            Platform.runLater(() -> afficherErreur("Lien manquant",
                "Aucun lien de session disponible.\nContactez votre enseignant.", null));
            return false;
        }
        try {
            Desktop.getDesktop().browse(new URI(lien));
            return true;
        } catch (Exception e) {
            // Fallback : copier dans le presse-papier
            copierDansPressePapier(lien);
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Lien copié");
                alert.setHeaderText("Impossible d'ouvrir le navigateur automatiquement");
                alert.setContentText(
                    "Le lien a été copié dans votre presse-papier.\n" +
                    "Collez-le dans votre navigateur pour rejoindre la session.\n\n" +
                    "Lien : " + lien);
                alert.showAndWait();
            });
            return false;
        }
    }

    // ── Utilitaires privés ────────────────────────────────────────────────────

    private static void afficherErreur(String titre, String message, String lienFallback) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(titre);
            alert.setHeaderText(titre);

            if (lienFallback != null && !lienFallback.isBlank()) {
                alert.setContentText(message + "\n\nVoulez-vous copier le lien manuellement ?");
                ButtonType copierBtn = new ButtonType("📋 Copier le lien");
                alert.getButtonTypes().setAll(copierBtn, ButtonType.CANCEL);
                alert.showAndWait().ifPresent(btn -> {
                    if (btn == copierBtn) copierDansPressePapier(lienFallback);
                });
            } else {
                alert.setContentText(message);
                alert.showAndWait();
            }
        });
    }

    private static void copierDansPressePapier(String texte) {
        javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
        cc.putString(texte);
        cb.setContent(cc);
    }
}

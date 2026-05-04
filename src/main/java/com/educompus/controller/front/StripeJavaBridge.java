package com.educompus.controller.front;

import javafx.application.Platform;

/**
 * Bridge public entre JavaScript (Stripe WebView) et Java.
 * DOIT être public pour que JavaFX WebView puisse y accéder via reflection.
 */
public class StripeJavaBridge {

    private final Runnable onSuccess;
    private final java.util.function.Consumer<String> onError;

    public StripeJavaBridge(Runnable onSuccess, java.util.function.Consumer<String> onError) {
        this.onSuccess = onSuccess;
        this.onError   = onError;
    }

    /** Appelé par Stripe.js quand le paiement est confirmé. */
    public void onPaymentCompleted(String paymentIntentId, String status) {
        System.out.println("[Stripe] onPaymentCompleted appelé : " + paymentIntentId + " / " + status);
        Platform.runLater(() -> {
            if (onSuccess != null) onSuccess.run();
        });
    }

    /** Appelé par Stripe.js en cas d'erreur. */
    public void onPaymentFailed(String message) {
        System.err.println("[Stripe] onPaymentFailed : " + message);
        Platform.runLater(() -> {
            if (onError != null) onError.accept(message);
        });
    }
}

package com.educompus.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

public class StripePaymentService {

    private final String secretKey;
    private final String publishableKey;
    private final String currency;

    public StripePaymentService() {
        // 1. Essayer le fichier stripe.properties en priorité
        java.util.Properties props = chargerProperties();

        this.secretKey      = lire(props, "STRIPE_SECRET_KEY");
        this.publishableKey = lire(props, "STRIPE_PUBLISHABLE_KEY");
        this.currency       = lireOptional(props, "STRIPE_CURRENCY", "eur");
        Stripe.apiKey = this.secretKey;
    }

    private java.util.Properties chargerProperties() {
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream is = getClass().getResourceAsStream("/stripe/stripe.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}
        return props;
    }

    /** Lit depuis le fichier .properties d'abord, puis System.getenv() en fallback */
    private static String lire(java.util.Properties props, String key) {
        String val = props.getProperty(key);
        if (val == null || val.isBlank() || val.startsWith("REMPLACE")) {
            val = System.getenv(key);
        }
        if (val == null || val.isBlank()) {
            throw new IllegalStateException("Clé Stripe manquante : " + key
                    + ". Configure src/main/resources/stripe/stripe.properties");
        }
        return val.trim();
    }

    private static String lireOptional(java.util.Properties props, String key, String def) {
        String val = props.getProperty(key);
        if (val == null || val.isBlank()) val = System.getenv(key);
        return (val == null || val.isBlank()) ? def : val.trim();
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public String getCurrency() {
        return currency;
    }

    public StripePaymentIntent createPaymentIntent(long amountMinorUnits, int userId) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountMinorUnits)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .putMetadata("userId", String.valueOf(userId))
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        return new StripePaymentIntent(intent.getId(), intent.getClientSecret());
    }

    public PaymentIntent retrieve(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    public boolean isSucceeded(String paymentIntentId) throws StripeException {
        PaymentIntent intent = retrieve(paymentIntentId);
        return "succeeded".equals(intent.getStatus());
    }

}


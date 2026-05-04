package com.educompus.service;

import com.educompus.util.MarketplaceConfig;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

public class StripePaymentService {

    private final String secretKey;
    private final String publishableKey;
    private final String currency;

    public StripePaymentService() {
        this.secretKey      = MarketplaceConfig.get("STRIPE_SECRET_KEY");
        this.publishableKey = MarketplaceConfig.get("STRIPE_PUBLISHABLE_KEY");
        this.currency       = MarketplaceConfig.getOrDefault("STRIPE_CURRENCY", "eur");
        Stripe.apiKey = this.secretKey;
    }

    public String getPublishableKey() { return publishableKey; }
    public String getCurrency()       { return currency; }

    public StripePaymentIntent createPaymentIntent(long amountMinorUnits, int userId) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountMinorUnits)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true).build())
                .putMetadata("userId", String.valueOf(userId))
                .build();
        PaymentIntent intent = PaymentIntent.create(params);
        return new StripePaymentIntent(intent.getId(), intent.getClientSecret());
    }

    public PaymentIntent retrieve(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    public boolean isSucceeded(String paymentIntentId) throws StripeException {
        return "succeeded".equals(retrieve(paymentIntentId).getStatus());
    }
}

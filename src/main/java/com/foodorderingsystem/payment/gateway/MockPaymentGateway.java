package com.foodorderingsystem.payment.gateway;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Safe classroom-only gateway simulator. No real card/bank transaction occurs.
 * Use the documented test tokens in the checkout page to force approval/decline.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {
    @Override
    public GatewayPaymentResponse process(GatewayPaymentRequest request) {
        String details = request.testPaymentDetails() == null ? "" : request.testPaymentDetails().trim();
        String upper = details.toUpperCase(Locale.ROOT);
        String transactionId = "TXN-" + UUID.randomUUID();

        if (upper.contains("DECLINE") || upper.contains("BANKFAIL") || upper.contains("INSUFFICIENT")) {
            return new GatewayPaymentResponse(false, transactionId,
                    "The simulated gateway declined this payment.");
        }

        if (request.paymentMethod() == null) {
            return new GatewayPaymentResponse(false, transactionId, "A payment method is required.");
        }

        return new GatewayPaymentResponse(true, transactionId,
                switch (request.paymentMethod()) {
                    case CARD -> "Simulated card gateway approved the payment.";
                    case ONLINE_WALLET -> "Simulated online banking/wallet gateway approved the payment.";
                    case CASH_ON_DELIVERY -> "Cash-on-delivery order accepted; payment is collected on delivery.";
                });
    }
}

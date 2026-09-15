package com.foodorderingsystem.payment.gateway;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public GatewayPaymentResponse process(GatewayPaymentRequest request) {
        String details = request.testPaymentDetails().toUpperCase(Locale.ROOT);
        String transactionId = "TXN-" + UUID.randomUUID();

        if (details.contains("DECLINE")) {
            return new GatewayPaymentResponse(false, transactionId,
                    "The test gateway declined this payment.");
        }
        return new GatewayPaymentResponse(true, transactionId, "Approved by the test gateway.");
    }
}

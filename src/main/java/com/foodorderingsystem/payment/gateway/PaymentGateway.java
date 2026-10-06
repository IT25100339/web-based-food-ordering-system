package com.foodorderingsystem.payment.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {
    GatewayPaymentResponse process(GatewayPaymentRequest request);

    /** Simulates a gateway refund without contacting a real bank/card network. */
    default GatewayPaymentResponse refund(String transactionId, BigDecimal amount) {
        return new GatewayPaymentResponse(true, "REF-" + transactionId,
                "Refund approved by the simulated gateway.");
    }
}

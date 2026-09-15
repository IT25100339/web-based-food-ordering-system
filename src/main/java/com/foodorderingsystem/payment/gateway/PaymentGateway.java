package com.foodorderingsystem.payment.gateway;

public interface PaymentGateway {
    GatewayPaymentResponse process(GatewayPaymentRequest request);
}

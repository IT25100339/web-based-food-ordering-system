package com.foodorderingsystem.payment.gateway;

public record GatewayPaymentResponse(boolean approved, String transactionId, String message) {
}

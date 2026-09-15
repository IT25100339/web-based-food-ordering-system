package com.foodorderingsystem.payment.gateway;

import com.foodorderingsystem.payment.entity.PaymentMethod;

import java.math.BigDecimal;

public record GatewayPaymentRequest(String orderId, BigDecimal amount,
                                    PaymentMethod paymentMethod, String testPaymentDetails) {
}

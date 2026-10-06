package com.foodorderingsystem.payment.strategy;

import org.springframework.stereotype.Component;

@Component
public class CardPaymentStrategy implements PaymentStrategy {
    public static final String METHOD_CODE = "CARD";
    private static final double GATEWAY_FEE_RATE = 0.02;

    @Override
    public double calculateFee(double orderAmount) {
        return Math.round(orderAmount * GATEWAY_FEE_RATE * 100) / 100.0;
    }

    @Override
    public String getLabel() { return "Card Payment"; }
}

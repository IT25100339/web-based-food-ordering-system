package com.foodorderingsystem.payment.strategy;

import org.springframework.stereotype.Component;

@Component
public class CashOnDeliveryStrategy implements PaymentStrategy {
    public static final String METHOD_CODE = "CASH_ON_DELIVERY";

    @Override
    public double calculateFee(double orderAmount) { return 0.0; }

    @Override
    public String getLabel() { return "Cash on Delivery"; }
}

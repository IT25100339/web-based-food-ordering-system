package com.foodorderingsystem.payment.strategy;

/** Strategy pattern: payment methods encapsulate their own fee and label rules. */
public interface PaymentStrategy {
    double calculateFee(double orderAmount);
    String getLabel();
}

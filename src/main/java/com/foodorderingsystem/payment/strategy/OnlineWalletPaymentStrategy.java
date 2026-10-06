package com.foodorderingsystem.payment.strategy;

import org.springframework.stereotype.Component;

@Component
public class OnlineWalletPaymentStrategy implements PaymentStrategy {
    public static final String METHOD_CODE = "ONLINE_WALLET";
    private static final double GATEWAY_FEE_RATE = 0.01;

    @Override
    public double calculateFee(double orderAmount) {
        return Math.round(orderAmount * GATEWAY_FEE_RATE * 100) / 100.0;
    }

    @Override
    public String getLabel() { return "Online Wallet / Banking"; }
}

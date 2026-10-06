package com.foodorderingsystem.payment.strategy;

import com.foodorderingsystem.payment.entity.PaymentMethod;
import org.springframework.stereotype.Component;

/** Factory pattern: centralizes selection of the correct payment strategy. */
@Component
public class PaymentStrategyFactory {
    private final CardPaymentStrategy cardPaymentStrategy;
    private final OnlineWalletPaymentStrategy onlineWalletPaymentStrategy;
    private final CashOnDeliveryStrategy cashOnDeliveryStrategy;

    public PaymentStrategyFactory(CardPaymentStrategy cardPaymentStrategy,
                                  OnlineWalletPaymentStrategy onlineWalletPaymentStrategy,
                                  CashOnDeliveryStrategy cashOnDeliveryStrategy) {
        this.cardPaymentStrategy = cardPaymentStrategy;
        this.onlineWalletPaymentStrategy = onlineWalletPaymentStrategy;
        this.cashOnDeliveryStrategy = cashOnDeliveryStrategy;
    }

    public PaymentStrategy getStrategy(PaymentMethod method) {
        if (method == null) {
            throw new IllegalArgumentException("Payment method is required.");
        }
        return switch (method) {
            case CARD -> cardPaymentStrategy;
            case ONLINE_WALLET -> onlineWalletPaymentStrategy;
            case CASH_ON_DELIVERY -> cashOnDeliveryStrategy;
        };
    }
}

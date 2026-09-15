package com.foodorderingsystem.payment.entity;

public enum PaymentMethod {
    CARD("Card"),
    MOBILE_WALLET("Mobile Wallet"),
    BANK_TRANSFER("Bank Transfer");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

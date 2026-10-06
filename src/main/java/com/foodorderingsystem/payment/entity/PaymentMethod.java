package com.foodorderingsystem.payment.entity;

public enum PaymentMethod {
    CARD("Card"),
    ONLINE_WALLET("Online Wallet / Banking"),
    CASH_ON_DELIVERY("Cash on Delivery");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

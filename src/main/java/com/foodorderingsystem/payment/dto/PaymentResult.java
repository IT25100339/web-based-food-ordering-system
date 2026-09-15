package com.foodorderingsystem.payment.dto;

import com.foodorderingsystem.payment.entity.PaymentMethod;
import com.foodorderingsystem.payment.entity.Payment;
import com.foodorderingsystem.payment.entity.Receipt;

import java.math.BigDecimal;

public record PaymentResult(boolean successful, String message, String orderId,
                            BigDecimal amount, PaymentMethod paymentMethod,
                            String transactionId, String receiptNumber) {

    public static PaymentResult success(PaymentRequest request, String transactionId, String receiptNumber) {
        return new PaymentResult(true, "Payment completed successfully.", request.getOrderId(),
                request.getAmount(), request.getPaymentMethod(), transactionId, receiptNumber);
    }

    public static PaymentResult failure(PaymentRequest request, String message, String transactionId) {
        return new PaymentResult(false, message, request.getOrderId(), request.getAmount(),
                request.getPaymentMethod(), transactionId, null);
    }

    public static PaymentResult success(Payment payment, Receipt receipt) {
        return new PaymentResult(true, "Payment completed successfully.", payment.getOrderId(),
                payment.getAmount(), payment.getPaymentMethod(), payment.getTransactionId(),
                receipt.getReceiptNumber());
    }
}

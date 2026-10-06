package com.foodorderingsystem.payment.dto;

import com.foodorderingsystem.payment.entity.Payment;
import com.foodorderingsystem.payment.entity.PaymentMethod;
import com.foodorderingsystem.payment.entity.Receipt;

public record PaymentResult(boolean successful, String message, Long orderId,
                            Double amount, PaymentMethod paymentMethod,
                            String transactionId, String receiptNumber) {
    public static PaymentResult success(Long orderId, Double amount, PaymentMethod method,
                                        String transactionId, String receiptNumber) {
        return new PaymentResult(true, "Payment completed successfully.", orderId, amount, method,
                transactionId, receiptNumber);
    }

    public static PaymentResult failure(Long orderId, Double amount, PaymentMethod method,
                                        String message, String transactionId) {
        return new PaymentResult(false, message, orderId, amount, method, transactionId, null);
    }

    public static PaymentResult success(Payment payment, Receipt receipt) {
        return new PaymentResult(true, "Payment completed successfully.",
                payment.getOrder() == null ? null : payment.getOrder().getOrderId(),
                payment.getAmount(), payment.getPaymentMethod(), payment.getTransactionId(),
                receipt == null ? null : receipt.getReceiptNumber());
    }
}

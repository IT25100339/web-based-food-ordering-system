package com.foodorderingsystem.payment.dto;

import com.foodorderingsystem.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class PaymentRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    private BigDecimal amount;

    @NotNull(message = "Please select a payment method")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "Payment details are required")
    @Size(max = 150, message = "Payment details must be 150 characters or fewer")
    private String testPaymentDetails;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getTestPaymentDetails() { return testPaymentDetails; }
    public void setTestPaymentDetails(String testPaymentDetails) {
        this.testPaymentDetails = testPaymentDetails == null ? null : testPaymentDetails.trim();
    }
}

package com.foodorderingsystem.payment.dto;

import com.foodorderingsystem.payment.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class PaymentRequest {

    @NotBlank(message = "Order ID is required")
    @Size(max = 100, message = "Order ID must be 100 characters or fewer")
    private String orderId;

    @NotNull(message = "Amount due is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @DecimalMax(value = "1000000.00", message = "Amount must not exceed 1000000.00")
    @Digits(integer = 10, fraction = 2, message = "Amount must have no more than two decimal places")
    private BigDecimal amount;

    @NotNull(message = "Please select a payment method")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "Test payment details are required")
    @Size(max = 150, message = "Test payment details must be 150 characters or fewer")
    private String testPaymentDetails;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId == null ? null : orderId.trim(); }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getTestPaymentDetails() { return testPaymentDetails; }
    public void setTestPaymentDetails(String testPaymentDetails) { this.testPaymentDetails = testPaymentDetails; }
}

package com.foodorderingsystem.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RefundRequest {
    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than zero")
    private Double amount;

    @NotBlank(message = "Refund reason is required")
    @Size(max = 255, message = "Reason must be 255 characters or fewer")
    private String reason;

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason == null ? null : reason.trim(); }
}

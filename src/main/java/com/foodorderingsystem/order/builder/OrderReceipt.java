package com.foodorderingsystem.order.builder;

import java.util.ArrayList;
import java.util.List;

// The finished "product" the Builder pattern constructs step by step - a plain summary
// object with no business logic of its own, just the pieces OrderReceiptBuilder assembled.
public class OrderReceipt {

    private Long orderId;
    private String customerName;
    private String deliveryAddress;
    private final List<String> lineItems = new ArrayList<>();
    private double subtotal;
    private double deliveryFee;
    private double processingFee;
    private double grandTotal;
    private String paymentMethod;
    private String status;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public List<String> getLineItems() { return lineItems; }
    public void addLineItem(String line) { lineItems.add(line); }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }

    public double getProcessingFee() { return processingFee; }
    public void setProcessingFee(double processingFee) { this.processingFee = processingFee; }

    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

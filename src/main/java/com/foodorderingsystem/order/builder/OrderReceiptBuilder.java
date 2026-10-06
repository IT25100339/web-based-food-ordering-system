package com.foodorderingsystem.order.builder;

import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.entity.OrderItem;
import com.foodorderingsystem.payment.entity.Payment;

// Builder pattern: constructing a receipt needs several independent, optional steps
// (customer details, each line item, fees, payment info) that don't belong in the Order
// entity itself. Instead of one constructor with a dozen parameters, each withXxx() step
// returns "this" so calls can be chained, and build() hands back the finished OrderReceipt
// only once every step has run.
//
// NOT a Spring bean on purpose: a builder holds in-progress, mutable state (the half-built
// receipt) between its steps, so one shared singleton instance would let two concurrent
// requests corrupt each other's receipt. Callers just do `new OrderReceiptBuilder()` to get
// a fresh, private builder every time.
public class OrderReceiptBuilder {

    private OrderReceipt receipt;

    public OrderReceiptBuilder start() {
        receipt = new OrderReceipt();
        return this;
    }

    public OrderReceiptBuilder withOrderDetails(Order order) {
        receipt.setOrderId(order.getOrderId());
        receipt.setDeliveryAddress(order.getDeliveryAddress());
        receipt.setStatus(order.getStatus());
        receipt.setCustomerName(order.getCustomer() != null ? order.getCustomer().getName() : "Guest");
        receipt.setSubtotal(order.getTotalAmount() != null ? order.getTotalAmount() : 0.0);
        return this;
    }

    public OrderReceiptBuilder withLineItems(Order order) {
        for (OrderItem item : order.getItems()) {
            String name = item.getMenuItem() != null ? item.getMenuItem().getName() : "Item";
            receipt.addLineItem(item.getQuantity() + " x " + name + " - Rs. "
                    + (item.getPriceAtOrderTime() * item.getQuantity()));
        }
        return this;
    }

    // Payment is optional - an order that hasn't been paid for yet simply skips this step
    public OrderReceiptBuilder withPayment(Payment payment) {
        if (payment == null) {
            return this;
        }
        receipt.setDeliveryFee(payment.getDeliveryFee() != null ? payment.getDeliveryFee() : 0.0);
        receipt.setProcessingFee(payment.getProcessingFee() != null ? payment.getProcessingFee() : 0.0);
        receipt.setPaymentMethod(payment.getMethod());
        return this;
    }

    public OrderReceiptBuilder calculateGrandTotal() {
        receipt.setGrandTotal(receipt.getSubtotal() + receipt.getDeliveryFee() + receipt.getProcessingFee());
        return this;
    }

    public OrderReceipt build() {
        return receipt;
    }
}

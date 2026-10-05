package com.foodorderingsystem.delivery.observer;

import com.foodorderingsystem.delivery.entity.Delivery;
import org.springframework.stereotype.Component;

// Concrete observer #1: simulates notifying the customer (e.g. an SMS/push notification)
// whenever their delivery's status moves forward. In a real deployment this would call an
// SMS/email/push service instead of just logging.
@Component
public class CustomerNotificationObserver implements DeliveryObserver {

    @Override
    public void onStatusChanged(Delivery delivery, String oldStatus, String newStatus) {
        String customerName = delivery.getOrder() != null && delivery.getOrder().getCustomer() != null
                ? delivery.getOrder().getCustomer().getName() : "Customer";
        System.out.println("[Notify Customer] " + customerName + " - your order #"
                + (delivery.getOrder() != null ? delivery.getOrder().getOrderId() : "?")
                + " is now " + newStatus);
    }
}

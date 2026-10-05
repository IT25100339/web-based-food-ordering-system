package com.foodorderingsystem.delivery.observer;

import com.foodorderingsystem.delivery.entity.Delivery;

// Observer pattern: DeliveryService (the "Subject") holds a list of these and notifies every
// one of them whenever a delivery's status changes. Each observer reacts in its own way -
// one tells the customer, another logs it for the admin dashboard - without DeliveryService
// needing to know who's listening or how many observers exist.
public interface DeliveryObserver {
    void onStatusChanged(Delivery delivery, String oldStatus, String newStatus);
}

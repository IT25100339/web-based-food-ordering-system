package com.foodorderingsystem.delivery.observer;

import com.foodorderingsystem.delivery.entity.Delivery;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

// Concrete observer #2: keeps a short in-memory activity log so the admin dashboard can show
// "what just happened" without polling the delivery table - this is the same notification
// event as CustomerNotificationObserver, just reacted to differently (log vs. notify).
@Component
public class AdminNotificationObserver implements DeliveryObserver {

    private static final int MAX_LOG_SIZE = 15;
    private final LinkedList<String> recentActivity = new LinkedList<>();
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    @Override
    public void onStatusChanged(Delivery delivery, String oldStatus, String newStatus) {
        Long orderId = delivery.getOrder() != null ? delivery.getOrder().getOrderId() : null;
        String riderName = delivery.getRider() != null ? delivery.getRider().getName() : "Unassigned";
        String entry = "[" + LocalDateTime.now().format(timeFmt) + "] Order #" + orderId
                + " - " + (oldStatus == null ? "created" : oldStatus + " -> " + newStatus)
                + " (Rider: " + riderName + ")";

        synchronized (recentActivity) {
            recentActivity.addFirst(entry);
            while (recentActivity.size() > MAX_LOG_SIZE) {
                recentActivity.removeLast();
            }
        }
    }

    public List<String> getRecentActivity() {
        synchronized (recentActivity) {
            return Collections.unmodifiableList(new LinkedList<>(recentActivity));
        }
    }
}

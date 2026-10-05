package com.foodorderingsystem.delivery.service;

import com.foodorderingsystem.delivery.entity.Delivery;
import com.foodorderingsystem.delivery.entity.Rider;
import com.foodorderingsystem.delivery.observer.DeliveryObserver;
import com.foodorderingsystem.delivery.repository.DeliveryRepository;
import com.foodorderingsystem.feedback.entity.Feedback;
import com.foodorderingsystem.feedback.service.FeedbackService;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeliveryService {

    // Flat commission paid to the rider for every order they deliver -
    // 20% of the Rs.250 flat delivery fee charged to the customer (see payment.service.PaymentService.DELIVERY_FEE).
    // Kept as its own constant here (rather than reading PaymentService) to avoid a circular
    // dependency, since PaymentService already depends on DeliveryService.
    public static final double RIDER_COMMISSION_PER_DELIVERY = 50.0;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private RiderService riderService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FeedbackService feedbackService;

    // Observer pattern: Spring injects every DeliveryObserver bean (CustomerNotificationObserver,
    // AdminNotificationObserver) into this list. DeliveryService (the Subject) doesn't know or
    // care what each observer does with a status change - it just loops through and notifies them.
    @Autowired
    private List<DeliveryObserver> deliveryObservers;

    private void notifyObservers(Delivery delivery, String oldStatus, String newStatus) {
        for (DeliveryObserver observer : deliveryObservers) {
            observer.onStatusChanged(delivery, oldStatus, newStatus);
        }
    }

    public Delivery createDeliveryForOrder(Order order) {
        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setDeliveryStatus("PENDING");
        Delivery saved = deliveryRepository.save(delivery);
        notifyObservers(saved, null, "PENDING");
        return saved;
    }

    public List<Delivery> getAllDeliveries() {
        return deliveryRepository.findAll();
    }

    // Orders waiting to be picked up by any available rider (self-service, like Uber Eats/PickMe)
    public List<Delivery> getPendingDeliveries() {
        return deliveryRepository.findByDeliveryStatus("PENDING");
    }

    public List<Delivery> getDeliveriesForRider(Rider rider) {
        return deliveryRepository.findByRiderOrderByAssignedAtDesc(rider);
    }

    public Delivery getById(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery not found with id: " + id));
    }

    public Delivery getByOrderId(Long orderId) {
        return deliveryRepository.findByOrder_OrderId(orderId).orElse(null);
    }

    public Delivery assignRider(Long deliveryId, Long riderId) {
        Delivery delivery = getById(deliveryId);

        // guard against two riders accepting the same delivery at the same time
        // (e.g. two browser tabs, one stale) - only a still-PENDING delivery can be accepted
        if (!"PENDING".equals(delivery.getDeliveryStatus())) {
            throw new IllegalStateException("This delivery has already been accepted by another rider.");
        }

        Rider rider = riderService.getRiderById(riderId);

        String oldStatus = delivery.getDeliveryStatus();
        delivery.setRider(rider);
        delivery.setDeliveryStatus("ASSIGNED");
        delivery.setAssignedAt(LocalDateTime.now());
        Delivery saved = deliveryRepository.save(delivery);

        rider.setStatus("ON_DELIVERY");
        riderService.updateRider(riderId, rider);

        orderService.updateStatus(delivery.getOrder().getOrderId(), "OUT_FOR_DELIVERY");

        notifyObservers(saved, oldStatus, "ASSIGNED");

        return saved;
    }

    public Delivery updateStatus(Long deliveryId, String newStatus) {
        Delivery delivery = getById(deliveryId);
        String oldStatus = delivery.getDeliveryStatus();
        delivery.setDeliveryStatus(newStatus);

        if (newStatus.equals("DELIVERED")) {
            delivery.setDeliveredAt(LocalDateTime.now());
            Rider rider = delivery.getRider();
            if (rider != null) {
                rider.setStatus("AVAILABLE");
                riderService.updateRider(rider.getRiderId(), rider);
            }
            orderService.updateStatus(delivery.getOrder().getOrderId(), "DELIVERED");
        }

        Delivery saved = deliveryRepository.save(delivery);
        notifyObservers(saved, oldStatus, newStatus);
        return saved;
    }

    // Rider performance stats shown on the rider dashboard: jobs done, active days,
    // earnings (commission-based), and average customer rating for their completed deliveries.
    public Map<String, Object> getRiderStats(Rider rider) {
        List<Delivery> completed = deliveryRepository.findByRiderAndDeliveryStatus(rider, "DELIVERED");

        LocalDate today = LocalDate.now();
        long todayCount = completed.stream()
                .filter(d -> d.getDeliveredAt() != null && d.getDeliveredAt().toLocalDate().isEqual(today))
                .count();

        long weekCount = completed.stream()
                .filter(d -> d.getDeliveredAt() != null
                        && d.getDeliveredAt().toLocalDate().get(IsoFields.WEEK_BASED_YEAR) == today.get(IsoFields.WEEK_BASED_YEAR)
                        && d.getDeliveredAt().toLocalDate().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) == today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
                .count();

        // distinct calendar dates the rider completed at least one delivery on
        long activeDays = completed.stream()
                .filter(d -> d.getDeliveredAt() != null)
                .map(d -> d.getDeliveredAt().toLocalDate())
                .distinct()
                .count();

        int totalJobs = completed.size();
        double totalEarnings = totalJobs * RIDER_COMMISSION_PER_DELIVERY;

        List<Long> orderIds = completed.stream()
                .map(d -> d.getOrder().getOrderId())
                .collect(Collectors.toList());

        List<Feedback> feedbackList = orderIds.isEmpty()
                ? List.of()
                : feedbackService.getAll().stream()
                .filter(f -> f.getOrder() != null && orderIds.contains(f.getOrder().getOrderId()))
                .collect(Collectors.toList());

        double avgRating = feedbackList.isEmpty()
                ? 0
                : feedbackList.stream().mapToInt(Feedback::getRating).average().orElse(0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", totalJobs);
        stats.put("todayJobs", todayCount);
        stats.put("weekJobs", weekCount);
        stats.put("activeDays", activeDays);
        stats.put("totalEarnings", totalEarnings);
        stats.put("commissionPerDelivery", RIDER_COMMISSION_PER_DELIVERY);
        stats.put("avgRating", Math.round(avgRating * 10) / 10.0);
        stats.put("ratingCount", feedbackList.size());
        stats.put("recentFeedback", feedbackList.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList()));

        return stats;
    }
}

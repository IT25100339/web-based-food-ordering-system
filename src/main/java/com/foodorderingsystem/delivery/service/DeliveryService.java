package com.foodorderingsystem.delivery.service;

import com.foodorderingsystem.delivery.entity.Delivery;
import com.foodorderingsystem.delivery.entity.Rider;
import com.foodorderingsystem.delivery.repository.DeliveryRepository;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeliveryService {

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private RiderService riderService;

    @Autowired
    private OrderService orderService;

    public Delivery createDeliveryForOrder(Order order) {
        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setDeliveryStatus("PENDING");
        return deliveryRepository.save(delivery);
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
        Rider rider = riderService.getRiderById(riderId);

        delivery.setRider(rider);
        delivery.setDeliveryStatus("ASSIGNED");
        delivery.setAssignedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);

        rider.setStatus("ON_DELIVERY");
        riderService.updateRider(riderId, rider);

        orderService.updateStatus(delivery.getOrder().getOrderId(), "OUT_FOR_DELIVERY");

        return delivery;
    }

    public Delivery updateStatus(Long deliveryId, String newStatus) {
        Delivery delivery = getById(deliveryId);
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

        return deliveryRepository.save(delivery);
    }
}

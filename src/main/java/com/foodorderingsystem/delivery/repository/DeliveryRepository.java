package com.foodorderingsystem.delivery.repository;

import com.foodorderingsystem.delivery.entity.Delivery;
import com.foodorderingsystem.delivery.entity.Rider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByDeliveryStatus(String status);
    Optional<Delivery> findByOrder_OrderId(Long orderId);
    List<Delivery> findByRiderOrderByAssignedAtDesc(Rider rider);
}

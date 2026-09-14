package com.foodorderingsystem.delivery.entity;

import com.foodorderingsystem.order.entity.Order;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
@Data
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryId;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "rider_id")
    private Rider rider;

    private String deliveryStatus = "PENDING"; // PENDING, ASSIGNED, OUT_FOR_DELIVERY, DELIVERED

    private LocalDateTime assignedAt;

    private LocalDateTime deliveredAt;
}

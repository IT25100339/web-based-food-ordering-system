package com.foodorderingsystem.order.entity;

import com.foodorderingsystem.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private String deliveryAddress;

    private Double totalAmount;

    // PENDING, CONFIRMED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
    private String status = "PENDING";

    private String paymentStatus = "UNPAID"; // UNPAID, PAID

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany
            (mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}

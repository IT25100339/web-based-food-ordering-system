package com.foodorderingsystem.feedback.entity;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.order.entity.Order;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Data
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feedbackId;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private Integer rating; // 1-5

    private String comment;

    private LocalDateTime createdAt = LocalDateTime.now();
}


package com.foodorderingsystem.order.repository;

import com.foodorderingsystem.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}

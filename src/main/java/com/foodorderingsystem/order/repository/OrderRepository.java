package com.foodorderingsystem.order.repository;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerOrderByCreatedAtDesc(Customer customer);
    List<Order> findAllByOrderByCreatedAtDesc();
    long countByCustomer(Customer customer);
}

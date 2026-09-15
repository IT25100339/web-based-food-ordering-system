package com.foodorderingsystem.payment.repository;

import com.foodorderingsystem.payment.entity.Payment;
import com.foodorderingsystem.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByOrderIdAndStatus(String orderId, PaymentStatus status);
    Optional<Payment> findByTransactionId(String transactionId);
}

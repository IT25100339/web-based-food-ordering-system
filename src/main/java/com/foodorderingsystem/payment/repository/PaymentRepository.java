package com.foodorderingsystem.payment.repository;

import com.foodorderingsystem.payment.entity.Payment;
import com.foodorderingsystem.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstByOrder_OrderIdAndStatusOrderByPaidAtDesc(Long orderId, PaymentStatus status);
    Optional<Payment> findFirstByOrder_OrderIdOrderByPaidAtDesc(Long orderId);
    List<Payment> findByOrder_OrderIdOrderByPaidAtDesc(Long orderId);
    List<Payment> findByOrder_Customer_CustomerIdOrderByPaidAtDesc(Long customerId);
    boolean existsByOrder_OrderIdAndStatus(Long orderId, PaymentStatus status);
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByStatus(PaymentStatus status);
}

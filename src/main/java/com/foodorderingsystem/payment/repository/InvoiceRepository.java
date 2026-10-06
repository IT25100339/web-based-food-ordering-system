package com.foodorderingsystem.payment.repository;

import com.foodorderingsystem.payment.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    Optional<Invoice> findByPayment_PaymentId(Long paymentId);
}

package com.foodorderingsystem.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_receipts")
@Data
@NoArgsConstructor
public class Receipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String receiptNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    public Receipt(String receiptNumber, Payment payment) {
        this.receiptNumber = receiptNumber;
        this.payment = payment;
        this.issuedAt = LocalDateTime.now();
    }
}

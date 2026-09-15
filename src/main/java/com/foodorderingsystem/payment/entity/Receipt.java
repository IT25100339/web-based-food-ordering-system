package com.foodorderingsystem.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_receipts")
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String receiptNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    protected Receipt() {
    }

    public Receipt(String receiptNumber, Payment payment) {
        this.receiptNumber = receiptNumber;
        this.payment = payment;
        this.issuedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getReceiptNumber() { return receiptNumber; }
    public Payment getPayment() { return payment; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
}

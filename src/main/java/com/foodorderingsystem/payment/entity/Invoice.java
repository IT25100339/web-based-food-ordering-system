package com.foodorderingsystem.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
public class Invoice {
    @Id
    @Column(name = "invoice_no", length = 60)
    private String invoiceNo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Column(nullable = false)
    private LocalDateTime issueDate;

    @Column(length = 255)
    private String fileUrl;

    public Invoice(String invoiceNo, Payment payment) {
        this.invoiceNo = invoiceNo;
        this.payment = payment;
        this.issueDate = LocalDateTime.now();
    }
}

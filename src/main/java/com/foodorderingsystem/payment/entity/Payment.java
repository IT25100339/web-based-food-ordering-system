package com.foodorderingsystem.payment.entity;

import com.foodorderingsystem.order.entity.Order;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Base order amount before payment/delivery fees. */
    @Column(nullable = false)
    private Double orderAmount;

    /** Final amount represented by this payment transaction. */
    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, unique = true, length = 60)
    private String transactionId;

    /** Only populated for a successful/refunded payment; allows failed retries. */
    @Column(unique = true)
    private Long successfulOrderId;

    @Column(nullable = false)
    private Double processingFee = 0.0;

    @Column(nullable = false)
    private Double deliveryFee = 0.0;

    @Column(nullable = false)
    private boolean verified = false;

    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    private LocalDateTime paidAt = LocalDateTime.now();

    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Receipt receipt;

    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Invoice invoice;

    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Refund refund;

    public Payment(Order order, Double orderAmount, Double amount, PaymentMethod paymentMethod,
                   PaymentStatus status, String transactionId, Double processingFee,
                   Double deliveryFee) {
        this.order = order;
        this.orderAmount = orderAmount;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.transactionId = transactionId;
        this.processingFee = processingFee == null ? 0.0 : processingFee;
        this.deliveryFee = deliveryFee == null ? 0.0 : deliveryFee;
        this.successfulOrderId = status == PaymentStatus.SUCCESS && order != null ? order.getOrderId() : null;
        this.paidAt = LocalDateTime.now();
    }

    /** Compatibility alias used by the V33 Builder pattern. */
    public String getMethod() {
        return paymentMethod == null ? null : paymentMethod.name();
    }

    /** Final amount including order amount, delivery fee and processing fee. */
    public double getTotalPaid() {
        return (orderAmount == null ? 0.0 : orderAmount)
                + (deliveryFee == null ? 0.0 : deliveryFee)
                + (processingFee == null ? 0.0 : processingFee);
    }
}

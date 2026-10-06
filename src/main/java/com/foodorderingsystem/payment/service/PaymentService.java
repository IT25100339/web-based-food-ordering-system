package com.foodorderingsystem.payment.service;

import com.foodorderingsystem.delivery.service.DeliveryService;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.service.OrderService;
import com.foodorderingsystem.payment.dto.PaymentRequest;
import com.foodorderingsystem.payment.dto.PaymentResult;
import com.foodorderingsystem.payment.entity.*;
import com.foodorderingsystem.payment.gateway.GatewayPaymentRequest;
import com.foodorderingsystem.payment.gateway.GatewayPaymentResponse;
import com.foodorderingsystem.payment.gateway.PaymentGateway;
import com.foodorderingsystem.payment.repository.*;
import com.foodorderingsystem.payment.strategy.PaymentStrategy;
import com.foodorderingsystem.payment.strategy.PaymentStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    public static final double DELIVERY_FEE = 250.0;

    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;
    private final InvoiceRepository invoiceRepository;
    private final RefundRepository refundRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final OrderService orderService;
    private final DeliveryService deliveryService;

    public PaymentService(PaymentRepository paymentRepository,
                          ReceiptRepository receiptRepository,
                          InvoiceRepository invoiceRepository,
                          RefundRepository refundRepository,
                          PaymentGateway paymentGateway,
                          PaymentStrategyFactory paymentStrategyFactory,
                          OrderService orderService,
                          DeliveryService deliveryService) {
        this.paymentRepository = paymentRepository;
        this.receiptRepository = receiptRepository;
        this.invoiceRepository = invoiceRepository;
        this.refundRepository = refundRepository;
        this.paymentGateway = paymentGateway;
        this.paymentStrategyFactory = paymentStrategyFactory;
        this.orderService = orderService;
        this.deliveryService = deliveryService;
    }

    @Transactional
    public synchronized PaymentResult processPayment(PaymentRequest request) {
        Order order = orderService.getById(request.getOrderId());

        if ("PAID".equalsIgnoreCase(order.getPaymentStatus()) ||
                "REFUNDED".equalsIgnoreCase(order.getPaymentStatus()) ||
                paymentRepository.existsByOrder_OrderIdAndStatus(order.getOrderId(), PaymentStatus.SUCCESS) ||
                paymentRepository.existsByOrder_OrderIdAndStatus(order.getOrderId(), PaymentStatus.REFUNDED)) {
            return PaymentResult.failure(order.getOrderId(), order.getTotalAmount(), request.getPaymentMethod(),
                    "A successful payment has already been recorded for this order.", null);
        }

        double orderAmount = order.getTotalAmount() == null ? 0.0 : order.getTotalAmount();
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(request.getPaymentMethod());
        double processingFee = strategy.calculateFee(orderAmount);
        double deliveryFee = DELIVERY_FEE;
        double finalAmount = roundMoney(orderAmount + processingFee + deliveryFee);

        GatewayPaymentResponse gatewayResponse = paymentGateway.process(
                new GatewayPaymentRequest(order.getOrderId().toString(),
                        BigDecimal.valueOf(finalAmount), request.getPaymentMethod(), request.getTestPaymentDetails()));

        PaymentStatus status = gatewayResponse.approved() ? PaymentStatus.SUCCESS : PaymentStatus.DECLINED;
        Payment payment = new Payment(order, orderAmount, finalAmount, request.getPaymentMethod(), status,
                gatewayResponse.transactionId(), processingFee, deliveryFee);
        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        if (!gatewayResponse.approved()) {
            return PaymentResult.failure(order.getOrderId(), finalAmount, request.getPaymentMethod(),
                    gatewayResponse.message(), gatewayResponse.transactionId());
        }

        String receiptNumber = "RCT-" + UUID.randomUUID();
        Receipt receipt = new Receipt(receiptNumber, savedPayment);
        receiptRepository.save(receipt);

        Invoice invoice = new Invoice("INV-" + UUID.randomUUID(), savedPayment);
        invoiceRepository.save(invoice);

        orderService.updatePaymentStatus(order.getOrderId(), "PAID");
        orderService.updateStatus(order.getOrderId(), "CONFIRMED");
        if (deliveryService.getByOrderId(order.getOrderId()) == null) {
            deliveryService.createDeliveryForOrder(order);
        }

        String message = request.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY
                ? "Cash-on-delivery order accepted. Payment will be collected on delivery."
                : "Payment completed successfully.";
        return new PaymentResult(true, message, order.getOrderId(), finalAmount,
                request.getPaymentMethod(), gatewayResponse.transactionId(), receiptNumber);
    }

    public Payment getByOrderId(Long orderId) {
        return paymentRepository.findFirstByOrder_OrderIdAndStatusOrderByPaidAtDesc(orderId, PaymentStatus.REFUNDED)
                .or(() -> paymentRepository.findFirstByOrder_OrderIdAndStatusOrderByPaidAtDesc(orderId, PaymentStatus.SUCCESS))
                .or(() -> paymentRepository.findFirstByOrder_OrderIdOrderByPaidAtDesc(orderId))
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
    }

    public Optional<Receipt> getReceiptByPaymentId(Long paymentId) {
        return receiptRepository.findByPayment_PaymentId(paymentId);
    }

    public Optional<Invoice> getInvoiceByPaymentId(Long paymentId) {
        return invoiceRepository.findByPayment_PaymentId(paymentId);
    }

    public Optional<Refund> getRefundByPaymentId(Long paymentId) {
        return refundRepository.findByPayment_PaymentId(paymentId);
    }

    public Optional<Payment> findByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll().stream()
                .sorted(Comparator.comparing(Payment::getPaidAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public List<Payment> getPaymentsBetween(LocalDate startDate, LocalDate endDate) {
        return getAllPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS || p.getStatus() == PaymentStatus.REFUNDED)
                .filter(p -> startDate == null || !p.getPaidAt().toLocalDate().isBefore(startDate))
                .filter(p -> endDate == null || !p.getPaidAt().toLocalDate().isAfter(endDate))
                .collect(Collectors.toList());
    }

    public List<Payment> getCustomerPayments(Long customerId) {
        return paymentRepository.findByOrder_Customer_CustomerIdOrderByPaidAtDesc(customerId);
    }

    public List<Payment> searchTransactions(String query, LocalDate startDate, LocalDate endDate,
                                            PaymentStatus status, PaymentMethod method) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return getAllPayments().stream()
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> method == null || p.getPaymentMethod() == method)
                .filter(p -> startDate == null || !p.getPaidAt().toLocalDate().isBefore(startDate))
                .filter(p -> endDate == null || !p.getPaidAt().toLocalDate().isAfter(endDate))
                .filter(p -> q.isEmpty() || matchesQuery(p, q))
                .collect(Collectors.toList());
    }

    private boolean matchesQuery(Payment p, String q) {
        String orderId = p.getOrder() == null ? "" : String.valueOf(p.getOrder().getOrderId());
        String transaction = p.getTransactionId() == null ? "" : p.getTransactionId();
        String email = p.getOrder() == null || p.getOrder().getCustomer() == null
                ? "" : p.getOrder().getCustomer().getEmail();
        String name = p.getOrder() == null || p.getOrder().getCustomer() == null
                ? "" : p.getOrder().getCustomer().getName();
        return orderId.toLowerCase(Locale.ROOT).contains(q)
                || transaction.toLowerCase(Locale.ROOT).contains(q)
                || email.toLowerCase(Locale.ROOT).contains(q)
                || name.toLowerCase(Locale.ROOT).contains(q);
    }

    @Transactional
    public Payment verifyPayment(Long paymentId) {
        Payment payment = getPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Only successful payments can be verified.");
        }
        payment.setVerified(true);
        payment.setVerifiedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Transactional
    public Refund refundPayment(Long paymentId, double requestedAmount, String reason) {
        Payment payment = getPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Only successful payments can be refunded.");
        }
        if (!payment.isVerified()) {
            throw new IllegalStateException("Verify the payment before processing a refund.");
        }
        if (refundRepository.findByPayment_PaymentId(paymentId).isPresent()) {
            throw new IllegalStateException("A refund already exists for this payment.");
        }
        double maxRefund = payment.getAmount() == null ? 0.0 : payment.getAmount();
        if (requestedAmount <= 0 || requestedAmount > maxRefund + 0.001) {
            throw new IllegalArgumentException("Refund amount must be greater than zero and no more than the payment amount.");
        }

        GatewayPaymentResponse gatewayResponse = paymentGateway.refund(
                payment.getTransactionId(), BigDecimal.valueOf(requestedAmount));
        if (!gatewayResponse.approved()) {
            throw new IllegalStateException(gatewayResponse.message());
        }

        Refund refund = new Refund(payment, roundMoney(requestedAmount), reason);
        refund.setRefundStatus(RefundStatus.APPROVED);
        refund.setProcessedAt(LocalDateTime.now());
        Refund saved = refundRepository.save(refund);

        if (requestedAmount + 0.001 >= maxRefund) {
            payment.setStatus(PaymentStatus.REFUNDED);
            if (payment.getOrder() != null) {
                orderService.updatePaymentStatus(payment.getOrder().getOrderId(), "REFUNDED");
            }
        }
        paymentRepository.save(payment);
        return saved;
    }

    /** CRUD delete is deliberately restricted to declined/failed attempts, preserving financial audit history. */
    @Transactional
    public void deleteDeclinedPayment(Long paymentId) {
        Payment payment = getPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.DECLINED) {
            throw new IllegalStateException("Only declined payment attempts may be deleted. Successful financial records are retained for auditability.");
        }
        paymentRepository.delete(payment);
    }

    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    public double getTotalRevenue() {
        return getAllPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS || p.getStatus() == PaymentStatus.REFUNDED)
                .mapToDouble(p -> {
                    double value = p.getTotalPaid();
                    if (p.getRefund() != null && p.getRefund().getRefundStatus() == RefundStatus.APPROVED) {
                        value -= p.getRefund().getAmount();
                    }
                    return value;
                })
                .sum();
    }

    public Map<String, Double> getRevenueBreakdown() {
        Map<String, Double> result = new LinkedHashMap<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            double total = getAllPayments().stream()
                    .filter(p -> p.getPaymentMethod() == method)
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCESS || p.getStatus() == PaymentStatus.REFUNDED)
                    .mapToDouble(p -> {
                        double value = p.getTotalPaid();
                        if (p.getRefund() != null && p.getRefund().getRefundStatus() == RefundStatus.APPROVED) {
                            value -= p.getRefund().getAmount();
                        }
                        return value;
                    }).sum();
            result.put(method.getDisplayName(), total);
        }
        return result;
    }

    public Map<String, Double> getMonthlyRevenue() {
        DateTimeFormatter keyFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MMM yyyy");
        Map<String, Double> sorted = new TreeMap<>();
        for (Payment p : getAllPayments()) {
            if (p.getStatus() != PaymentStatus.SUCCESS && p.getStatus() != PaymentStatus.REFUNDED) continue;
            double value = p.getTotalPaid();
            if (p.getRefund() != null && p.getRefund().getRefundStatus() == RefundStatus.APPROVED) {
                value -= p.getRefund().getAmount();
            }
            sorted.merge(p.getPaidAt().format(keyFmt), value, Double::sum);
        }
        Map<String, Double> result = new LinkedHashMap<>();
        sorted.forEach((key, value) -> result.put(java.time.YearMonth.parse(key).format(labelFmt), value));
        return result;
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

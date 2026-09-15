package com.foodorderingsystem.payment.service;

import com.foodorderingsystem.payment.dto.PaymentRequest;
import com.foodorderingsystem.payment.dto.PaymentResult;
import com.foodorderingsystem.payment.entity.PaymentMethod;
import com.foodorderingsystem.payment.entity.PaymentStatus;
import com.foodorderingsystem.payment.entity.Payment;
import com.foodorderingsystem.payment.repository.PaymentRepository;
import com.foodorderingsystem.payment.repository.ReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PaymentServiceTests {

    @Autowired private PaymentService paymentService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ReceiptRepository receiptRepository;

    @BeforeEach
    void cleanDatabase() {
        receiptRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    void savesSuccessfulPaymentAndReceipt() {
        PaymentResult result = paymentService.processPayment(request("ORD-1001", "4111 1111 1111 1111"));

        assertTrue(result.successful());
        assertNotNull(result.transactionId());
        assertNotNull(result.receiptNumber());
        assertTrue(paymentRepository.existsByOrderIdAndStatus("ORD-1001", PaymentStatus.SUCCESS));
        assertEquals(1, receiptRepository.count());
        Payment payment = paymentRepository.findAll().get(0);
        assertEquals("ORD-1001", payment.getOrderId());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertTrue(receiptRepository.findByPaymentId(payment.getId()).isPresent());
    }

    @Test
    void recordsDeclinedPaymentWithoutReceipt() {
        PaymentResult result = paymentService.processPayment(request("ORD-1002", "DECLINE"));

        assertFalse(result.successful());
        assertNotNull(result.transactionId());
        assertEquals(0, receiptRepository.count());
    }

    @Test
    void preventsSecondSuccessfulPaymentForOrder() {
        paymentService.processPayment(request("ORD-1003", "valid details"));
        PaymentResult duplicate = paymentService.processPayment(request("ORD-1003", "valid details"));

        assertFalse(duplicate.successful());
        assertTrue(duplicate.message().contains("already"));
    }

    @ParameterizedTest
    @EnumSource(PaymentMethod.class)
    void processesEverySupportedPaymentMethod(PaymentMethod paymentMethod) {
        PaymentRequest request = request("ORD-" + paymentMethod, "valid details");
        request.setPaymentMethod(paymentMethod);

        PaymentResult result = paymentService.processPayment(request);

        assertTrue(result.successful());
        assertEquals(paymentMethod, result.paymentMethod());
    }

    @Test
    void permitsSuccessfulRetryAfterDeclinedPayment() {
        PaymentResult declined = paymentService.processPayment(request("ORD-1004", "DECLINE"));
        PaymentResult retry = paymentService.processPayment(request("ORD-1004", "valid details"));

        assertFalse(declined.successful());
        assertTrue(retry.successful());
        assertTrue(paymentRepository.existsByOrderIdAndStatus("ORD-1004", PaymentStatus.SUCCESS));
        assertEquals(1, receiptRepository.count());
    }

    @Test
    void trimsOrderIdBeforeCheckingForDuplicatesAndSaving() {
        paymentService.processPayment(request("  ORD-1005  ", "valid details"));
        PaymentResult duplicate = paymentService.processPayment(request("ORD-1005", "valid details"));

        assertFalse(duplicate.successful());
        assertTrue(paymentRepository.existsByOrderIdAndStatus("ORD-1005", PaymentStatus.SUCCESS));
    }

    private PaymentRequest request(String orderId, String details) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setAmount(new BigDecimal("1500.00"));
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setTestPaymentDetails(details);
        return request;
    }
}

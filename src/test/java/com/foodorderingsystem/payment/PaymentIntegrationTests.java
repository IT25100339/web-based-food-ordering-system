package com.foodorderingsystem.payment;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.customer.repository.CustomerRepository;
import com.foodorderingsystem.delivery.entity.Delivery;
import com.foodorderingsystem.delivery.repository.DeliveryRepository;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.repository.OrderRepository;
import com.foodorderingsystem.order.service.OrderService;
import com.foodorderingsystem.payment.dto.PaymentRequest;
import com.foodorderingsystem.payment.dto.PaymentResult;
import com.foodorderingsystem.payment.entity.Payment;
import com.foodorderingsystem.payment.entity.PaymentMethod;
import com.foodorderingsystem.payment.entity.PaymentStatus;
import com.foodorderingsystem.payment.repository.PaymentRepository;
import com.foodorderingsystem.payment.repository.ReceiptRepository;
import com.foodorderingsystem.payment.repository.InvoiceRepository;
import com.foodorderingsystem.payment.repository.RefundRepository;
import com.foodorderingsystem.payment.entity.RefundStatus;
import com.foodorderingsystem.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PaymentIntegrationTests {

    @Autowired private PaymentService paymentService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private RefundRepository refundRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderService orderService;
    @Autowired private DeliveryRepository deliveryRepository;
    @Autowired private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setup() {
        deliveryRepository.deleteAll();
        refundRepository.deleteAll();
        invoiceRepository.deleteAll();
        receiptRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();

        testCustomer = new Customer();
        testCustomer.setName("Alice Test");
        testCustomer.setEmail("alice@test.com");
        testCustomer.setPassword("password123");
        testCustomer.setPhone("0771234567");
        testCustomer.setAddress("123 Main St");
        testCustomer = customerRepository.save(testCustomer);
    }

    private Order createTestOrder(double amount) {
        Order order = new Order();
        order.setCustomer(testCustomer);
        order.setDeliveryAddress("123 Main St, Colombo");
        order.setTotalAmount(amount);
        order.setStatus("PENDING");
        order.setPaymentStatus("UNPAID");
        return orderRepository.save(order);
    }

    @Test
    void testSuccessfulPaymentAndReceiptFlow() {
        Order order = createTestOrder(2500.00);

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(order.getOrderId());
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setTestPaymentDetails("4111 1111 1111 1111");

        PaymentResult result = paymentService.processPayment(request);

        // 1. Payment result
        assertTrue(result.successful());
        assertNotNull(result.transactionId());
        assertNotNull(result.receiptNumber());
        assertEquals(2800.00, result.amount());

        // 2. Order status transitions
        Order updatedOrder = orderService.getById(order.getOrderId());
        assertEquals("PAID", updatedOrder.getPaymentStatus());
        assertEquals("CONFIRMED", updatedOrder.getStatus());

        // 3. Receipt generated
        assertEquals(1, receiptRepository.count());
        assertEquals(1, invoiceRepository.count());
        assertTrue(receiptRepository.findByReceiptNumber(result.receiptNumber()).isPresent());

        // 4. Delivery created
        List<Delivery> deliveries = deliveryRepository.findAll();
        assertEquals(1, deliveries.size());
        assertEquals("PENDING", deliveries.get(0).getDeliveryStatus());
        assertEquals(order.getOrderId(), deliveries.get(0).getOrder().getOrderId());
    }

    @ParameterizedTest
    @EnumSource(PaymentMethod.class)
    void testEveryPaymentMethodSucceeds(PaymentMethod method) {
        Order order = createTestOrder(1200.00);

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(order.getOrderId());
        request.setPaymentMethod(method);
        request.setTestPaymentDetails("valid details");

        PaymentResult result = paymentService.processPayment(request);

        assertTrue(result.successful());
        assertEquals(method, result.paymentMethod());
    }

    @Test
    void testDeclinedPaymentFlow() {
        Order order = createTestOrder(1800.00);

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(order.getOrderId());
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setTestPaymentDetails("DECLINE_TEST_CARD");

        PaymentResult result = paymentService.processPayment(request);

        // 1. Result should be failure
        assertFalse(result.successful());
        assertNotNull(result.transactionId());
        assertNull(result.receiptNumber());

        // 2. Order should remain UNPAID and PENDING
        Order updatedOrder = orderService.getById(order.getOrderId());
        assertEquals("UNPAID", updatedOrder.getPaymentStatus());
        assertEquals("PENDING", updatedOrder.getStatus());

        // 3. No receipt generated
        assertEquals(0, receiptRepository.count());

        // 4. No delivery generated
        assertEquals(0, deliveryRepository.count());

        // 5. Payment record saved with DECLINED status for audit
        Payment payment = paymentRepository.findByTransactionId(result.transactionId()).orElse(null);
        assertNotNull(payment);
        assertEquals(PaymentStatus.DECLINED, payment.getStatus());
    }

    @Test
    void testRetryAfterDeclinedPayment() {
        Order order = createTestOrder(3000.00);

        // First attempt: DECLINED
        PaymentRequest request1 = new PaymentRequest();
        request1.setOrderId(order.getOrderId());
        request1.setPaymentMethod(PaymentMethod.CARD);
        request1.setTestPaymentDetails("DECLINE");
        PaymentResult result1 = paymentService.processPayment(request1);
        assertFalse(result1.successful());

        // Second attempt: SUCCESSFUL RETRY
        PaymentRequest request2 = new PaymentRequest();
        request2.setOrderId(order.getOrderId());
        request2.setPaymentMethod(PaymentMethod.ONLINE_WALLET);
        request2.setTestPaymentDetails("0779998888");
        PaymentResult result2 = paymentService.processPayment(request2);
        assertTrue(result2.successful());

        // Verify order is now PAID
        Order updatedOrder = orderService.getById(order.getOrderId());
        assertEquals("PAID", updatedOrder.getPaymentStatus());
        assertEquals("CONFIRMED", updatedOrder.getStatus());
        assertEquals(1, receiptRepository.count());
        assertEquals(1, deliveryRepository.count());
    }

    @Test
    void testDuplicateSuccessfulPaymentPrevention() {
        Order order = createTestOrder(1500.00);

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(order.getOrderId());
        request.setPaymentMethod(PaymentMethod.ONLINE_WALLET);
        request.setTestPaymentDetails("ACC-12345");

        PaymentResult first = paymentService.processPayment(request);
        assertTrue(first.successful());

        // Second attempt for the same order
        PaymentResult second = paymentService.processPayment(request);
        assertFalse(second.successful());
        assertTrue(second.message().contains("already"));

        // Delivery and receipt counts must remain exactly 1
        assertEquals(1, receiptRepository.count());
        assertEquals(1, deliveryRepository.count());
    }

    @Test
    void testAuthoritativeAmountEnforcedServerSide() {
        Order order = createTestOrder(4500.00);

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(order.getOrderId());
        // Malicious client tries to pay 1.00
        request.setAmount(BigDecimal.valueOf(1.00));
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setTestPaymentDetails("4111 1111 1111 1111");

        PaymentResult result = paymentService.processPayment(request);
        assertTrue(result.successful());

        // The authoritative amount from the order (4500.00) must be recorded
        assertEquals(4840.00, result.amount());
        Payment saved = paymentRepository.findByTransactionId(result.transactionId()).orElseThrow();
        assertEquals(4840.00, saved.getAmount());
    }
    @Test
    void testVerifyAndRefundFlow() {
        Order order = createTestOrder(1000.00);
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(order.getOrderId());
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setTestPaymentDetails("4111111111111111");

        PaymentResult result = paymentService.processPayment(request);
        assertTrue(result.successful());

        Payment payment = paymentRepository.findByTransactionId(result.transactionId()).orElseThrow();
        assertFalse(payment.isVerified());
        paymentService.verifyPayment(payment.getPaymentId());
        assertTrue(paymentService.getPayment(payment.getPaymentId()).isVerified());

        var refund = paymentService.refundPayment(payment.getPaymentId(), payment.getAmount(), "Customer cancellation");
        assertEquals(RefundStatus.APPROVED, refund.getRefundStatus());
        assertEquals(PaymentStatus.REFUNDED, paymentService.getPayment(payment.getPaymentId()).getStatus());
        assertEquals("REFUNDED", orderService.getById(order.getOrderId()).getPaymentStatus());
    }

}

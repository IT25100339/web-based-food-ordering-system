package com.foodorderingsystem.payment.service;

import com.foodorderingsystem.payment.dto.PaymentRequest;
import com.foodorderingsystem.payment.dto.PaymentResult;
import com.foodorderingsystem.payment.entity.Payment;
import com.foodorderingsystem.payment.entity.PaymentStatus;
import com.foodorderingsystem.payment.entity.Receipt;
import com.foodorderingsystem.payment.gateway.GatewayPaymentRequest;
import com.foodorderingsystem.payment.gateway.GatewayPaymentResponse;
import com.foodorderingsystem.payment.gateway.PaymentGateway;
import com.foodorderingsystem.payment.repository.PaymentRepository;
import com.foodorderingsystem.payment.repository.ReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;
    private final PaymentGateway paymentGateway;

    public PaymentService(PaymentRepository paymentRepository, ReceiptRepository receiptRepository,
                          PaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.receiptRepository = receiptRepository;
        this.paymentGateway = paymentGateway;
    }

    @Transactional
    public synchronized PaymentResult processPayment(PaymentRequest request) {
        request.setOrderId(request.getOrderId());
        if (paymentRepository.existsByOrderIdAndStatus(request.getOrderId(), PaymentStatus.SUCCESS)) {
            return PaymentResult.failure(request,
                    "A successful payment has already been recorded for this order.", null);
        }

        GatewayPaymentResponse gatewayResponse = paymentGateway.process(new GatewayPaymentRequest(
                request.getOrderId(), request.getAmount(), request.getPaymentMethod(), request.getTestPaymentDetails()));

        PaymentStatus status = gatewayResponse.approved() ? PaymentStatus.SUCCESS : PaymentStatus.DECLINED;
        Payment payment = new Payment(request.getOrderId(), request.getAmount(), request.getPaymentMethod(),
                status, gatewayResponse.transactionId());
        paymentRepository.saveAndFlush(payment);

        if (!gatewayResponse.approved()) {
            return PaymentResult.failure(request, gatewayResponse.message(), gatewayResponse.transactionId());
        }

        String receiptNumber = "RCT-" + UUID.randomUUID();
        receiptRepository.save(new Receipt(receiptNumber, payment));
        return PaymentResult.success(request, gatewayResponse.transactionId(), receiptNumber);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentResult> findSuccessfulPayment(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .flatMap(payment -> receiptRepository.findByPaymentId(payment.getId())
                        .map(receipt -> PaymentResult.success(payment, receipt)));
    }
}

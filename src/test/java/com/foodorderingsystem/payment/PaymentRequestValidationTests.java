package com.foodorderingsystem.payment;

import com.foodorderingsystem.payment.dto.PaymentRequest;
import com.foodorderingsystem.payment.entity.PaymentMethod;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentRequestValidationTests {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsBlankRequiredFields() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(null);
        request.setTestPaymentDetails("   ");
        request.setPaymentMethod(null);

        assertEquals(3, validator.validate(request).size());
    }

    @Test
    void acceptsValidRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(101L);
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setTestPaymentDetails("4111 1111 1111 1111");

        assertTrue(validator.validate(request).isEmpty());
    }
}

package com.foodorderingsystem.payment.dto;

import com.foodorderingsystem.payment.entity.PaymentMethod;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
        request.setOrderId(" ");
        request.setTestPaymentDetails(" ");

        assertEquals(4, validator.validate(request).size());
    }

    @Test
    void rejectsZeroAndNegativeAmounts() {
        assertTrue(validator.validate(requestWithAmount("0.00")).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("amount")));
        assertTrue(validator.validate(requestWithAmount("-5.00")).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("amount")));
    }

    @Test
    void rejectsAmountWithMoreThanTwoDecimalPlaces() {
        assertTrue(validator.validate(requestWithAmount("12.345")).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("amount")));
    }

    @Test
    void rejectsAmountAboveMaximum() {
        assertTrue(validator.validate(requestWithAmount("1000000.01")).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("amount")));
    }

    private PaymentRequest requestWithAmount(String amount) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId("ORD-VALID");
        request.setAmount(new BigDecimal(amount));
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setTestPaymentDetails("valid details");
        return request;
    }
}

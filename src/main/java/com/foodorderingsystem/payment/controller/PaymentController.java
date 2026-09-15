package com.foodorderingsystem.payment.controller;

import com.foodorderingsystem.payment.dto.PaymentRequest;
import com.foodorderingsystem.payment.dto.PaymentResult;
import com.foodorderingsystem.payment.entity.PaymentMethod;
import com.foodorderingsystem.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/new")
    public String paymentForm(Model model) {
        model.addAttribute("paymentRequest", new PaymentRequest());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "payment/form";
    }

    @GetMapping("/success")
    public String paymentSuccess(@RequestParam String transactionId, Model model) {
        return paymentService.findSuccessfulPayment(transactionId)
                .map(result -> {
                    model.addAttribute("payment", result);
                    return "payment/success";
                })
                .orElse("redirect:/payments/new");
    }

    @PostMapping
    public String processPayment(@Valid @ModelAttribute PaymentRequest paymentRequest,
                                 BindingResult bindingResult, Model model,
                                 HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "payment/form";
        }

        request.setAttribute("paymentRequest", paymentRequest);
        PaymentResult result = paymentService.processPayment(paymentRequest);
        if (result.successful()) {
            redirectAttributes.addAttribute("transactionId", result.transactionId());
            return "redirect:/payments/success";
        }
        model.addAttribute("payment", result);
        return "payment/failure";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDuplicatePayment(DataIntegrityViolationException exception,
                                         HttpServletRequest request, Model model) {
        PaymentRequest paymentRequest = (PaymentRequest) request.getAttribute("paymentRequest");
        if (paymentRequest == null) {
            return "redirect:/payments/new";
        }
        model.addAttribute("payment", PaymentResult.failure(paymentRequest,
                "A successful payment has already been recorded for this order.", null));
        return "payment/failure";
    }
}

package com.foodorderingsystem.payment.controller;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.customer.service.CustomerService;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.service.OrderService;
import com.foodorderingsystem.payment.dto.PaymentRequest;
import com.foodorderingsystem.payment.dto.PaymentResult;
import com.foodorderingsystem.payment.dto.RefundRequest;
import com.foodorderingsystem.payment.entity.*;
import com.foodorderingsystem.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/payment")
public class PaymentController {
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final CustomerService customerService;

    public PaymentController(PaymentService paymentService, OrderService orderService,
                             CustomerService customerService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.customerService = customerService;
    }

    @GetMapping("/checkout/{orderId}")
    public String showCheckout(@PathVariable Long orderId, HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        if (customer == null) return "redirect:/login";

        Order order = orderService.getById(orderId);
        if (order.getCustomer() == null || !order.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            return "redirect:/customer/dashboard";
        }
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus()) ||
                "REFUNDED".equalsIgnoreCase(order.getPaymentStatus())) {
            return "redirect:/payment/invoice/" + orderId;
        }

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setAmount(java.math.BigDecimal.valueOf(order.getTotalAmount() == null ? 0.0 : order.getTotalAmount()));
        model.addAttribute("order", order);
        model.addAttribute("paymentRequest", request);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("deliveryFee", PaymentService.DELIVERY_FEE);
        return "payment/checkout";
    }

    @PostMapping("/pay")
    public String pay(@Valid @ModelAttribute("paymentRequest") PaymentRequest paymentRequest,
                      BindingResult bindingResult, HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        if (customer == null) return "redirect:/login";
        if (paymentRequest.getOrderId() == null) return "redirect:/customer/dashboard";

        Order order = orderService.getById(paymentRequest.getOrderId());
        if (order.getCustomer() == null || !order.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            return "redirect:/customer/dashboard";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("order", order);
            model.addAttribute("paymentMethods", PaymentMethod.values());
            model.addAttribute("deliveryFee", PaymentService.DELIVERY_FEE);
            return "payment/checkout";
        }

        try {
            PaymentResult result = paymentService.processPayment(paymentRequest);
            if (result.successful()) return "redirect:/payment/invoice/" + order.getOrderId();
            model.addAttribute("order", order);
            model.addAttribute("payment", result);
            return "payment/failure";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("order", order);
            model.addAttribute("error", ex.getMessage());
            return "payment/failure";
        }
    }

    @GetMapping("/invoice/{orderId}")
    public String showInvoice(@PathVariable Long orderId, HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        if (customer == null && !isAdmin) return "redirect:/login";

        Order order = orderService.getById(orderId);
        if (customer != null && (order.getCustomer() == null ||
                !order.getCustomer().getCustomerId().equals(customer.getCustomerId()))) {
            return "redirect:/customer/dashboard";
        }
        Payment payment = paymentService.getByOrderId(orderId);
        model.addAttribute("order", order);
        model.addAttribute("payment", payment);
        paymentService.getReceiptByPaymentId(payment.getPaymentId()).ifPresent(r -> model.addAttribute("receipt", r));
        paymentService.getInvoiceByPaymentId(payment.getPaymentId()).ifPresent(i -> model.addAttribute("invoice", i));
        paymentService.getRefundByPaymentId(payment.getPaymentId()).ifPresent(r -> model.addAttribute("refund", r));
        return "payment/invoice";
    }

    @GetMapping("/history")
    public String customerHistory(HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        if (customer == null) return "redirect:/login";
        model.addAttribute("payments", paymentService.getCustomerPayments(customer.getCustomerId()));
        return "payment/history";
    }

    @GetMapping("/transactions")
    public String transactions(@RequestParam(required = false) String q,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                               @RequestParam(required = false) PaymentStatus status,
                               @RequestParam(required = false) PaymentMethod method,
                               HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";
        List<Payment> payments = paymentService.searchTransactions(q, startDate, endDate, status, method);
        model.addAttribute("payments", payments);
        model.addAttribute("q", q);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedMethod", method);
        model.addAttribute("statuses", PaymentStatus.values());
        model.addAttribute("methods", PaymentMethod.values());
        return "payment/transactions";
    }

    @GetMapping("/transactions/{paymentId}")
    public String transactionDetail(@PathVariable Long paymentId, HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";
        Payment payment = paymentService.getPayment(paymentId);
        model.addAttribute("payment", payment);
        model.addAttribute("refundRequest", new RefundRequest());
        paymentService.getReceiptByPaymentId(paymentId).ifPresent(r -> model.addAttribute("receipt", r));
        paymentService.getInvoiceByPaymentId(paymentId).ifPresent(i -> model.addAttribute("invoice", i));
        paymentService.getRefundByPaymentId(paymentId).ifPresent(r -> model.addAttribute("refund", r));
        return "payment/transaction-detail";
    }

    @PostMapping("/verify/{paymentId}")
    public String verify(@PathVariable Long paymentId, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) return "redirect:/login";
        try {
            paymentService.verifyPayment(paymentId);
            redirectAttributes.addFlashAttribute("success", "Payment verified successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/payment/transactions/" + paymentId;
    }

    @PostMapping("/refund/{paymentId}")
    public String refund(@PathVariable Long paymentId,
                         @Valid @ModelAttribute("refundRequest") RefundRequest request,
                         BindingResult bindingResult,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) return "redirect:/login";
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Enter a valid refund amount and reason.");
            return "redirect:/payment/transactions/" + paymentId;
        }
        try {
            paymentService.refundPayment(paymentId, request.getAmount(), request.getReason());
            redirectAttributes.addFlashAttribute("success", "Refund processed successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/payment/transactions/" + paymentId;
    }

    @PostMapping("/transactions/{paymentId}/delete")
    public String deleteDeclined(@PathVariable Long paymentId, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) return "redirect:/login";
        try {
            paymentService.deleteDeclinedPayment(paymentId);
            redirectAttributes.addFlashAttribute("success", "Declined payment attempt deleted.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/payment/transactions";
    }

    @GetMapping("/reports")
    public String salesReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                              HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";
        List<Payment> payments = paymentService.getPaymentsBetween(startDate, endDate);
        double totalRevenue = payments.stream().mapToDouble(this::netValue).sum();
        model.addAttribute("payments", payments);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("totalCustomers", customerService.getTotalCustomerCount());
        model.addAttribute("totalOrders", orderService.getAllOrders().size());
        model.addAttribute("topItems", orderService.getTopSellingItems());
        model.addAttribute("revenueBreakdown", paymentService.getRevenueBreakdown());
        return "payment/reports";
    }

    @GetMapping("/reports/export")
    public void exportCsv(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                           HttpSession session, HttpServletResponse response) throws Exception {
        if (!isAdmin(session)) {
            response.sendRedirect("/login");
            return;
        }
        List<Payment> payments = paymentService.getPaymentsBetween(startDate, endDate);
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"sales_report.csv\"");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        PrintWriter writer = response.getWriter();
        writer.println("Payment ID,Order ID,Amount,Delivery Fee,Processing Fee,Method,Status,Verified,Transaction ID,Date");
        for (Payment p : payments) {
            writer.printf("%d,%d,%.2f,%.2f,%.2f,%s,%s,%s,%s,%s%n",
                    p.getPaymentId(), p.getOrder().getOrderId(), p.getAmount(), p.getDeliveryFee(),
                    p.getProcessingFee(), csv(p.getPaymentMethod().getDisplayName()), p.getStatus(),
                    p.isVerified(), csv(p.getTransactionId()), p.getPaidAt().format(fmt));
        }
        writer.flush();
    }

    private String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    private double netValue(Payment payment) {
        double value = payment.getTotalPaid();
        if (payment.getRefund() != null && payment.getRefund().getRefundStatus() == RefundStatus.APPROVED) {
            value -= payment.getRefund().getAmount();
        }
        return value;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDuplicatePayment(DataIntegrityViolationException exception, Model model) {
        model.addAttribute("error", "The payment could not be saved because it would create a duplicate financial record.");
        return "payment/failure";
    }
}

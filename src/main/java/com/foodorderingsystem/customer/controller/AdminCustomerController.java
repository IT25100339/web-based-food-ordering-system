package com.foodorderingsystem.customer.controller;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.customer.service.CustomerService;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.repository.OrderRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin-only Customer management (CRUD).
 * Customer self-service (register/login/own profile) stays in CustomerController -
 * this is the separate admin-facing screen for browsing and managing ALL customers.
 */
@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OrderRepository orderRepository;

    private boolean notAdmin(HttpSession session) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        return isAdmin == null || !isAdmin;
    }

    // ---- Read: list all customers, with order count + total spent per customer ----
    @GetMapping
    public String listCustomers(HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        List<Customer> customers = customerService.getAllCustomers();

        Map<Long, Long> orderCounts = new LinkedHashMap<>();
        Map<Long, Double> totalSpent = new LinkedHashMap<>();
        for (Customer c : customers) {
            List<Order> orders = orderRepository.findByCustomerOrderByCreatedAtDesc(c);
            orderCounts.put(c.getCustomerId(), (long) orders.size());
            double spent = orders.stream()
                    .filter(o -> o.getTotalAmount() != null)
                    .mapToDouble(Order::getTotalAmount)
                    .sum();
            totalSpent.put(c.getCustomerId(), spent);
        }

        model.addAttribute("customers", customers);
        model.addAttribute("orderCounts", orderCounts);
        model.addAttribute("totalSpent", totalSpent);
        return "admin/customer-list";
    }

    // ---- Read: view one customer's full details + their order history ----
    @GetMapping("/view/{id}")
    public String viewCustomer(@PathVariable Long id, HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        Customer customer = customerService.getById(id);
        List<Order> orders = orderRepository.findByCustomerOrderByCreatedAtDesc(customer);
        double totalSpent = orders.stream()
                .filter(o -> o.getTotalAmount() != null)
                .mapToDouble(Order::getTotalAmount)
                .sum();
        model.addAttribute("customer", customer);
        model.addAttribute("orders", orders);
        model.addAttribute("totalSpent", totalSpent);
        return "admin/customer-view";
    }

    // ---- Create ----
    @GetMapping("/new")
    public String showCreateForm(HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("customer", new Customer());
        model.addAttribute("isNew", true);
        return "admin/customer-form";
    }

    @PostMapping("/save")
    public String createCustomer(HttpSession session,
                                 @Valid @ModelAttribute("customer") Customer customer,
                                 BindingResult result, Model model,
                                 RedirectAttributes redirectAttributes) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("isNew", true);
            return "admin/customer-form";
        }
        if (customerService.emailExists(customer.getEmail())) {
            model.addAttribute("isNew", true);
            model.addAttribute("emailError", "This email is already registered to another customer.");
            return "admin/customer-form";
        }
        customerService.register(customer);
        redirectAttributes.addFlashAttribute("success", "Customer added.");
        return "redirect:/admin/customers";
    }

    // ---- Update ----
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("customer", customerService.getById(id));
        model.addAttribute("isNew", false);
        return "admin/customer-form";
    }

    @PostMapping("/update/{id}")
    public String updateCustomer(@PathVariable Long id, HttpSession session,
                                 @RequestParam String name,
                                 @RequestParam String email,
                                 @RequestParam(required = false) String phone,
                                 @RequestParam(required = false) String address,
                                 Model model, RedirectAttributes redirectAttributes) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }

        if (name == null || name.isBlank() || email == null || email.isBlank()) {
            Customer preview = customerService.getById(id);
            model.addAttribute("customer", preview);
            model.addAttribute("isNew", false);
            model.addAttribute("error", "Name and email can't be empty.");
            return "admin/customer-form";
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            Customer preview = customerService.getById(id);
            model.addAttribute("customer", preview);
            model.addAttribute("isNew", false);
            model.addAttribute("error", "Enter a valid email address.");
            return "admin/customer-form";
        }
        if (phone != null && !phone.isBlank() && !phone.matches("^[0-9]{9,15}$")) {
            Customer preview = customerService.getById(id);
            model.addAttribute("customer", preview);
            model.addAttribute("isNew", false);
            model.addAttribute("error", "Phone number must contain 9-15 digits only.");
            return "admin/customer-form";
        }

        try {
            customerService.adminUpdate(id, name, email, phone, address);
        } catch (DataIntegrityViolationException ex) {
            Customer preview = customerService.getById(id);
            model.addAttribute("customer", preview);
            model.addAttribute("isNew", false);
            model.addAttribute("error", "That email is already used by another customer.");
            return "admin/customer-form";
        }
        redirectAttributes.addFlashAttribute("success", "Customer details updated.");
        return "redirect:/admin/customers";
    }

    // ---- Delete ----
    @PostMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        try {
            customerService.deleteCustomer(id);
            redirectAttributes.addFlashAttribute("success", "Customer deleted.");
        } catch (DataIntegrityViolationException ex) {
            // customer has orders/feedback referencing them (FK constraint) - block the delete
            redirectAttributes.addFlashAttribute("error",
                    "Can't delete this customer - they have existing orders/feedback linked to their account.");
        }
        return "redirect:/admin/customers";
    }
}

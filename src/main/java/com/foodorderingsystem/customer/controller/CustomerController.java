package com.foodorderingsystem.customer.controller;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.customer.service.CustomerService;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.repository.OrderRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customer/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("customer") Customer customer, BindingResult result) {
        if (result.hasErrors()) {
            return "customer/register";
        }
        customerService.register(customer);
        return "redirect:/login";
    }

    // Login/logout are handled by the unified AuthController (/login, /logout)

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        if (customer == null) {
            return "redirect:/login";
        }
        List<Order> orders = orderRepository.findByCustomerOrderByCreatedAtDesc(customer);
        model.addAttribute("customer", customer);
        model.addAttribute("orders", orders);
        model.addAttribute("orderCount", orders.size());
        return "customer/dashboard";
    }

    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        if (customer == null) {
            return "redirect:/login";
        }
        model.addAttribute("customer", customer);
        return "customer/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute("customer") Customer formCustomer,
                                BindingResult result, HttpSession session, Model model) {
        Customer sessionCustomer = (Customer) session.getAttribute("loggedInCustomer");
        if (sessionCustomer == null) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            return "customer/profile";
        }
        Customer updated = customerService.update(sessionCustomer.getCustomerId(), formCustomer);
        session.setAttribute("loggedInCustomer", updated);
        model.addAttribute("customer", updated);
        model.addAttribute("success", true);
        return "customer/profile";
    }
}

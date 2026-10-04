package com.foodorderingsystem.auth.controller;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.customer.service.CustomerService;
import com.foodorderingsystem.delivery.entity.Rider;
import com.foodorderingsystem.delivery.service.RiderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private RiderService riderService;

    // Hardcoded admin credentials (Spring Security will replace this later)
    @Value("${app.admin.username:admin@quickbite.com}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String role, Model model) {
        model.addAttribute("role", role);
        return "login";
    }

    // Portal entry points from the homepage - each preselects the matching role tab
    @GetMapping("/customer/login")
    public String customerLoginPortal(Model model) {
        model.addAttribute("role", "CUSTOMER");
        return "login";
    }

    @GetMapping("/delivery/login")
    public String riderLoginPortal(Model model) {
        model.addAttribute("role", "RIDER");
        return "login";
    }

    @GetMapping("/admin/login")
    public String adminLoginPortal(Model model) {
        model.addAttribute("role", "ADMIN");
        return "login";
    }

    // Single restaurant setup: the restaurant is managed by the system admin,
    // so the "Restaurant Portal" routes into the admin login for now.
    @GetMapping("/restaurant/login")
    public String restaurantLoginPortal(Model model) {
        model.addAttribute("role", "ADMIN");
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String role,
                               @RequestParam String identifier,
                               @RequestParam String password,
                               HttpSession session, Model model) {

        switch (role) {
            case "CUSTOMER":
                Customer customer = customerService.login(identifier, password);
                if (customer == null) {
                    model.addAttribute("error", "Invalid email or password");
                    model.addAttribute("role", role);
                    return "login";
                }
                session.setAttribute("loggedInCustomer", customer);
                return "redirect:/customer/dashboard";

            case "RIDER":
                Rider rider = riderService.login(identifier, password);
                if (rider == null) {
                    model.addAttribute("error", "Invalid phone number or password");
                    model.addAttribute("role", role);
                    return "login";
                }
                session.setAttribute("loggedInRider", rider);
                return "redirect:/delivery/rider-dashboard";

            case "ADMIN":
                if (identifier.equals(adminUsername) && password.equals(adminPassword)) {
                    session.setAttribute("isAdmin", true);
                    return "redirect:/admin/dashboard";
                }
                model.addAttribute("error", "Invalid admin credentials");
                model.addAttribute("role", role);
                return "login";

            default:
                model.addAttribute("error", "Please select a valid role");
                return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}

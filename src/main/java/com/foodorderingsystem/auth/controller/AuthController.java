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
    @Value("${app.admin.username:admin@expressmeal.com}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String role,
                                @RequestParam(required = false) String redirect, Model model) {
        model.addAttribute("role", role);
        model.addAttribute("redirect", redirect);
        return "login";
    }

    // Portal entry points from the homepage - each preselects the matching role tab
    @GetMapping("/customer/login")
    public String customerLoginPortal(@RequestParam(required = false) String redirect, Model model) {
        model.addAttribute("role", "CUSTOMER");
        model.addAttribute("redirect", redirect);
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
                               @RequestParam(required = false) String redirect,
                               HttpSession session, Model model) {

        switch (role) {
            case "CUSTOMER":
                Customer customer = customerService.login(identifier, password);
                if (customer == null) {
                    model.addAttribute("error", "Invalid email or password");
                    model.addAttribute("role", role);
                    model.addAttribute("redirect", redirect);
                    return "login";
                }
                session.setAttribute("loggedInCustomer", customer);
                // send them back to where they came from (e.g. the cart) instead of always the dashboard
                if (redirect != null && redirect.startsWith("/")) {
                    return "redirect:" + redirect;
                }
                return "redirect:/customer/dashboard";

            case "RIDER":
                Rider rider = riderService.login(identifier, password);
                if (rider == null) {
                    model.addAttribute("error", "Invalid phone number or password");
                    model.addAttribute("role", role);
                    model.addAttribute("redirect", redirect);
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
                model.addAttribute("redirect", redirect);
                return "login";

            default:
                model.addAttribute("error", "Please select a valid role");
                model.addAttribute("redirect", redirect);
                return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
    // ---- Forgot password (customer only) ----

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {
        String token = customerService.generateResetToken(email);
        model.addAttribute("email", email);
        if (token != null) {
            // Email sending isn't configured in this dev project - the "sent" link is shown directly
            // here so the flow can be demoed end-to-end without a real mail server.
            model.addAttribute("resetLink", "/reset-password/" + token);
        }
        // Same confirmation message whether or not the email exists (don't reveal registered emails)
        return "forgot-password-sent";
    }

    @GetMapping("/reset-password/{token}")
    public String showResetPasswordForm(@PathVariable String token, Model model) {
        boolean valid = customerService.findByValidResetToken(token).isPresent();
        model.addAttribute("token", token);
        model.addAttribute("valid", valid);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                       @RequestParam String newPassword,
                                       @RequestParam String confirmPassword,
                                       Model model) {
        var customerOpt = customerService.findByValidResetToken(token);
        if (customerOpt.isEmpty()) {
            model.addAttribute("token", token);
            model.addAttribute("valid", false);
            return "reset-password";
        }
        if (!newPassword.matches("^(?=.*[0-9])(?=.*[!@#$%^&*(),.?\":{}|<>_\\-]).{8,}$")) {
            model.addAttribute("token", token);
            model.addAttribute("valid", true);
            model.addAttribute("error", "Password must be at least 8 characters and include a number and a special character.");
            return "reset-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("valid", true);
            model.addAttribute("error", "Passwords don't match.");
            return "reset-password";
        }
        customerService.resetPassword(customerOpt.get(), newPassword);
        return "redirect:/login?reset=success";
    }

    // ---- Forgot password (rider) ----

    @GetMapping("/rider/forgot-password")
    public String showRiderForgotPasswordForm() {
        return "rider-forgot-password";
    }

    @PostMapping("/rider/forgot-password")
    public String processRiderForgotPassword(@RequestParam String phoneNumber, Model model) {
        String token = riderService.generateResetToken(phoneNumber);
        model.addAttribute("phoneNumber", phoneNumber);
        if (token != null) {
            // Email/SMS sending isn't configured in this dev project - the "sent" link is shown
            // directly here so the flow can be demoed end-to-end without a real gateway.
            model.addAttribute("resetLink", "/rider/reset-password/" + token);
        }
        // Same confirmation message whether or not the phone number exists (don't reveal registered riders)
        return "rider-forgot-password-sent";
    }

    @GetMapping("/rider/reset-password/{token}")
    public String showRiderResetPasswordForm(@PathVariable String token, Model model) {
        boolean valid = riderService.findByValidResetToken(token).isPresent();
        model.addAttribute("token", token);
        model.addAttribute("valid", valid);
        return "rider-reset-password";
    }

    @PostMapping("/rider/reset-password")
    public String processRiderResetPassword(@RequestParam String token,
                                            @RequestParam String newPassword,
                                            @RequestParam String confirmPassword,
                                            Model model) {
        var riderOpt = riderService.findByValidResetToken(token);
        if (riderOpt.isEmpty()) {
            model.addAttribute("token", token);
            model.addAttribute("valid", false);
            return "rider-reset-password";
        }
        if (!newPassword.matches("^(?=.*[0-9])(?=.*[!@#$%^&*(),.?\":{}|<>_\\-]).{8,}$")) {
            model.addAttribute("token", token);
            model.addAttribute("valid", true);
            model.addAttribute("error", "Password must be at least 8 characters and include a number and a special character.");
            return "rider-reset-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("valid", true);
            model.addAttribute("error", "Passwords don't match.");
            return "rider-reset-password";
        }
        riderService.resetPassword(riderOpt.get(), newPassword);
        return "redirect:/login?role=RIDER&reset=success";
    }
}

package com.foodorderingsystem.delivery.controller;

import com.foodorderingsystem.delivery.entity.Rider;
import com.foodorderingsystem.delivery.service.DeliveryService;
import com.foodorderingsystem.delivery.service.RiderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/delivery")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private RiderService riderService;

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    private boolean notAdmin(HttpSession session) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        return isAdmin == null || !isAdmin;
    }

    // ---- Admin: view/manage all deliveries ----
    @GetMapping("/list")
    public String listDeliveries(HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("deliveries", deliveryService.getAllDeliveries());
        return "delivery/delivery-list";
    }

    @GetMapping("/assign/{deliveryId}")
    public String showAssignForm(@PathVariable Long deliveryId, HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("delivery", deliveryService.getById(deliveryId));
        model.addAttribute("availableRiders", riderService.getAvailableRiders());
        return "delivery/assign-rider";
    }

    @PostMapping("/assign")
    public String assignRider(@RequestParam Long deliveryId, @RequestParam Long riderId, HttpSession session) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        deliveryService.assignRider(deliveryId, riderId);
        return "redirect:/delivery/list";
    }

    // ---- Rider: their own dashboard, delivery tracking for assigned orders ----
    @GetMapping("/rider-dashboard")
    public String riderDashboard(HttpSession session, Model model) {
        Rider rider = (Rider) session.getAttribute("loggedInRider");
        if (rider == null) {
            return "redirect:/login";
        }
        model.addAttribute("rider", rider);
        model.addAttribute("deliveries", deliveryService.getDeliveriesForRider(rider));
        model.addAttribute("pendingDeliveries", deliveryService.getPendingDeliveries());
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        model.addAttribute("stats", deliveryService.getRiderStats(rider));
        return "delivery/rider-dashboard";
    }

    // ---- Rider: self-accept a pending order (no admin needed) ----
    @PostMapping("/accept/{deliveryId}")
    public String acceptDelivery(@PathVariable Long deliveryId, HttpSession session, RedirectAttributes redirectAttributes) {
        Rider rider = (Rider) session.getAttribute("loggedInRider");
        if (rider == null) {
            return "redirect:/login";
        }
        try {
            deliveryService.assignRider(deliveryId, rider.getRiderId());
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/delivery/rider-dashboard";
    }

    // Status update - used by both admin (delivery-list page) and rider (rider-dashboard page)
    @PostMapping("/{id}/update-status")
    public String updateStatus(@PathVariable Long id, @RequestParam String status, HttpSession session) {
        Rider rider = (Rider) session.getAttribute("loggedInRider");
        boolean isAdminSession = !notAdmin(session);
        if (rider == null && !isAdminSession) {
            return "redirect:/login";
        }
        deliveryService.updateStatus(id, status);
        if (rider != null) {
            return "redirect:/delivery/rider-dashboard";
        }
        return "redirect:/delivery/list";
    }

    // ---- Rider: view/edit own profile (name, phone/username, vehicle, password) ----
    @GetMapping("/rider-profile")
    public String riderProfile(HttpSession session, Model model) {
        Rider sessionRider = (Rider) session.getAttribute("loggedInRider");
        if (sessionRider == null) {
            return "redirect:/login";
        }
        model.addAttribute("rider", riderService.getRiderById(sessionRider.getRiderId()));
        return "delivery/rider-profile";
    }

    @PostMapping("/rider-profile")
    public String updateRiderProfile(HttpSession session,
                                     @RequestParam String name,
                                     @RequestParam String phoneNumber,
                                     @RequestParam(required = false) String vehicleNumber,
                                     @RequestParam(required = false) String currentPassword,
                                     @RequestParam(required = false) String newPassword,
                                     @RequestParam(required = false) String confirmPassword,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        Rider sessionRider = (Rider) session.getAttribute("loggedInRider");
        if (sessionRider == null) {
            return "redirect:/login";
        }

        if (name == null || name.isBlank() || phoneNumber == null || phoneNumber.isBlank()) {
            model.addAttribute("rider", riderService.getRiderById(sessionRider.getRiderId()));
            model.addAttribute("error", "Name and phone number can't be empty.");
            return "delivery/rider-profile";
        }
        if (!phoneNumber.matches("^[0-9]{10}$")) {
            model.addAttribute("rider", riderService.getRiderById(sessionRider.getRiderId()));
            model.addAttribute("error", "Phone number must be exactly 10 digits.");
            return "delivery/rider-profile";
        }
        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            model.addAttribute("rider", riderService.getRiderById(sessionRider.getRiderId()));
            model.addAttribute("error", "Vehicle number can't be empty.");
            return "delivery/rider-profile";
        }

        if (newPassword != null && !newPassword.isBlank()) {
            if (!newPassword.matches("^(?=.*[0-9])(?=.*[!@#$%^&*(),.?\":{}|<>_\\-]).{8,}$")) {
                model.addAttribute("rider", riderService.getRiderById(sessionRider.getRiderId()));
                model.addAttribute("error", "New password must be at least 8 characters and include a number and a special character.");
                return "delivery/rider-profile";
            }
            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("rider", riderService.getRiderById(sessionRider.getRiderId()));
                model.addAttribute("error", "New password and confirmation don't match.");
                return "delivery/rider-profile";
            }
        }

        try {
            Rider updated = riderService.updateProfile(
                    sessionRider.getRiderId(), name, phoneNumber, vehicleNumber, currentPassword, newPassword);
            session.setAttribute("loggedInRider", updated);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
            return "redirect:/delivery/rider-profile";
        } catch (IllegalArgumentException | ConstraintViolationException ex) {
            model.addAttribute("rider", riderService.getRiderById(sessionRider.getRiderId()));
            model.addAttribute("error", ex.getMessage());
            return "delivery/rider-profile";
        }
    }
}

package com.foodorderingsystem.delivery.controller;

import com.foodorderingsystem.delivery.entity.Rider;
import com.foodorderingsystem.delivery.service.DeliveryService;
import com.foodorderingsystem.delivery.service.RiderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/delivery")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private RiderService riderService;

    // ---- Admin: view/manage all deliveries ----
    @GetMapping("/list")
    public String listDeliveries(HttpSession session, Model model) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        model.addAttribute("deliveries", deliveryService.getAllDeliveries());
        return "delivery/delivery-list";
    }

    @GetMapping("/assign/{deliveryId}")
    public String showAssignForm(@PathVariable Long deliveryId, Model model) {
        model.addAttribute("delivery", deliveryService.getById(deliveryId));
        model.addAttribute("availableRiders", riderService.getAvailableRiders());
        return "delivery/assign-rider";
    }

    @PostMapping("/assign")
    public String assignRider(@RequestParam Long deliveryId, @RequestParam Long riderId) {
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
        return "delivery/rider-dashboard";
    }

    // ---- Rider: self-accept a pending order (no admin needed) ----
    @PostMapping("/accept/{deliveryId}")
    public String acceptDelivery(@PathVariable Long deliveryId, HttpSession session) {
        Rider rider = (Rider) session.getAttribute("loggedInRider");
        if (rider == null) {
            return "redirect:/login";
        }
        deliveryService.assignRider(deliveryId, rider.getRiderId());
        return "redirect:/delivery/rider-dashboard";
    }

    // Status update - used by both admin (delivery-list page) and rider (rider-dashboard page)
    @PostMapping("/{id}/update-status")
    public String updateStatus(@PathVariable Long id, @RequestParam String status, HttpSession session) {
        deliveryService.updateStatus(id, status);
        if (session.getAttribute("loggedInRider") != null) {
            return "redirect:/delivery/rider-dashboard";
        }
        return "redirect:/delivery/list";
    }
}

package com.foodorderingsystem.admin.controller;

import com.foodorderingsystem.delivery.service.DeliveryService;
import com.foodorderingsystem.order.service.OrderService;
import com.foodorderingsystem.payment.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private DeliveryService deliveryService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        model.addAttribute("totalOrders", orderService.getAllOrders().size());
        model.addAttribute("totalRevenue", paymentService.getTotalRevenue());

        // Monthly revenue trend (line chart)
        Map<String, Double> monthlyRevenue = paymentService.getMonthlyRevenue();
        model.addAttribute("monthLabels", new ArrayList<>(monthlyRevenue.keySet()));
        model.addAttribute("monthRevenues", new ArrayList<>(monthlyRevenue.values()));

        // Order status breakdown (doughnut chart)
        Map<String, Long> statusBreakdown = orderService.getOrderStatusBreakdown();
        model.addAttribute("statusLabels", new ArrayList<>(statusBreakdown.keySet()));
        model.addAttribute("statusCounts", new ArrayList<>(statusBreakdown.values()));

        // Top-selling menu items (bar chart)
        Map<String, Integer> topItems = orderService.getTopSellingItems();
        model.addAttribute("topItemLabels", new ArrayList<>(topItems.keySet()));
        model.addAttribute("topItemCounts", new ArrayList<>(topItems.values()));

        // Weekly order volume (bar chart)
        Map<String, Long> weeklyVolume = orderService.getWeeklyOrderVolume();
        model.addAttribute("weekLabels", new ArrayList<>(weeklyVolume.keySet()));
        model.addAttribute("weekCounts", new ArrayList<>(weeklyVolume.values()));

        // Rider performance (bar chart)
        Map<String, Long> riderPerf = deliveryService.getRiderPerformance();
        model.addAttribute("riderLabels", new ArrayList<>(riderPerf.keySet()));
        model.addAttribute("riderCounts", new ArrayList<>(riderPerf.values()));

        return "admin/dashboard";
    }
}

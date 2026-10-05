package com.foodorderingsystem.admin.controller;

import com.foodorderingsystem.admin.singleton.AppSettings;
import com.foodorderingsystem.customer.service.CustomerService;
import com.foodorderingsystem.delivery.observer.AdminNotificationObserver;
import com.foodorderingsystem.delivery.service.RiderService;
import com.foodorderingsystem.order.service.OrderService;
import com.foodorderingsystem.payment.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private RiderService riderService;

    // Observer pattern: this bean has been quietly collecting delivery status-change
    // events (see DeliveryService.notifyObservers). The dashboard just reads its log.
    @Autowired
    private AdminNotificationObserver adminNotificationObserver;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        model.addAttribute("totalOrders", orderService.getAllOrders().size());
        model.addAttribute("totalRevenue", paymentService.getTotalRevenue());
        model.addAttribute("totalCustomers", customerService.getTotalCustomerCount());
        model.addAttribute("availableRiders", riderService.getAvailableRiders().size());
        model.addAttribute("revenueBreakdown", paymentService.getRevenueBreakdown());

        // Monthly revenue trend (line chart)
        Map<String, Double> monthlyRevenue = paymentService.getMonthlyRevenue();
        model.addAttribute("monthLabels", new ArrayList<>(monthlyRevenue.keySet()));
        model.addAttribute("monthRevenues", new ArrayList<>(monthlyRevenue.values()));

        // Order status breakdown (doughnut chart)
        Map<String, Long> statusBreakdown = orderService.getOrderStatusBreakdown();
        model.addAttribute("statusLabels", new ArrayList<>(statusBreakdown.keySet()));
        model.addAttribute("statusCounts", new ArrayList<>(statusBreakdown.values()));

        // Observer pattern: recent delivery activity collected by AdminNotificationObserver
        model.addAttribute("recentActivity", adminNotificationObserver.getRecentActivity());

        // Singleton pattern: the one shared AppSettings instance - same object the home page reads
        model.addAttribute("appSettings", AppSettings.getInstance());

        return "admin/dashboard";
    }

    // Admin edits the site-wide announcement / maintenance mode, both stored on the single
    // shared AppSettings instance (Singleton pattern) - every page that calls
    // AppSettings.getInstance() afterwards sees the change immediately, with no database needed.
    @PostMapping("/settings")
    public String updateSettings(@RequestParam(required = false) String announcementMessage,
                                 @RequestParam(required = false, defaultValue = "false") boolean maintenanceMode,
                                 HttpSession session, RedirectAttributes redirectAttributes) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        AppSettings.getInstance().setAnnouncementMessage(announcementMessage);
        AppSettings.getInstance().setMaintenanceMode(maintenanceMode);
        redirectAttributes.addFlashAttribute("success", "Site settings updated.");
        return "redirect:/admin/dashboard";
    }
}

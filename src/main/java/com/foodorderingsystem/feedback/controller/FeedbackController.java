package com.foodorderingsystem.feedback.controller;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.feedback.entity.Feedback;
import com.foodorderingsystem.feedback.service.FeedbackService;
import com.foodorderingsystem.order.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private OrderService orderService;

    @GetMapping("/add/{orderId}")
    public String showForm(@PathVariable Long orderId, Model model) {
        model.addAttribute("order", orderService.getById(orderId));
        return "feedback/add";
    }

    @PostMapping("/save")
    public String save(@RequestParam Long orderId, @RequestParam Integer rating,
                       @RequestParam String comment, HttpSession session) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");

        Feedback feedback = new Feedback();
        feedback.setOrder(orderService.getById(orderId));
        feedback.setCustomer(customer);
        feedback.setRating(rating);
        feedback.setComment(comment);

        feedbackService.save(feedback);
        return "redirect:/feedback/list";
    }

    @GetMapping("/list")
    public String listFeedback(Model model) {
        model.addAttribute("feedbackList", feedbackService.getAll());
        model.addAttribute("averageRating", feedbackService.getAverageRating());
        return "feedback/list";
    }
}

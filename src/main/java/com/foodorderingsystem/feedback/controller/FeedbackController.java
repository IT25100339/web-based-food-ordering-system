package com.foodorderingsystem.feedback.controller;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.feedback.entity.Feedback;
import com.foodorderingsystem.feedback.service.FeedbackService;
import com.foodorderingsystem.feedback.strategy.FeedbackFilterFactory;
import com.foodorderingsystem.feedback.strategy.FeedbackFilterStrategy;
import com.foodorderingsystem.order.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FeedbackFilterFactory feedbackFilterFactory;

    // ---- Create ----
    @GetMapping("/add/{orderId}")
    public String showForm(@PathVariable Long orderId, Model model) {
        // this order already has feedback (order-to-feedback is one-to-one) - send them to edit instead
        Feedback existing = feedbackService.findByOrderId(orderId).orElse(null);
        if (existing != null) {
            return "redirect:/feedback/edit/" + existing.getFeedbackId();
        }
        model.addAttribute("order", orderService.getById(orderId));
        return "feedback/add";
    }

    @PostMapping("/save")
    public String save(@RequestParam Long orderId, @RequestParam(required = false) Integer rating,
                       @RequestParam(required = false) String comment, HttpSession session,
                       RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");

        // guard against double-submit / two tabs racing to feedback the same order
        Feedback existing = feedbackService.findByOrderId(orderId).orElse(null);
        if (existing != null) {
            redirectAttributes.addFlashAttribute("info", "You've already left feedback for this order - here it is.");
            return "redirect:/feedback/edit/" + existing.getFeedbackId();
        }

        // real server-side validation - never trust the client-side checks alone
        if (rating == null || rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("error", "Please select a rating between 1 and 5.");
            return "redirect:/feedback/add/" + orderId;
        }
        if (comment == null || comment.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Please write a short comment about your experience.");
            return "redirect:/feedback/add/" + orderId;
        }

        Feedback feedback = new Feedback();
        feedback.setOrder(orderService.getById(orderId));
        feedback.setCustomer(customer);
        feedback.setRating(rating);
        feedback.setComment(comment.trim());

        try {
            feedbackService.save(feedback);
        } catch (DataIntegrityViolationException ex) {
            // fallback safety net in case of a race condition past the check above
            redirectAttributes.addFlashAttribute("error", "You've already left feedback for this order.");
            return "redirect:/feedback/list";
        }
        redirectAttributes.addFlashAttribute("success", "Thanks! Your feedback has been submitted.");
        return "redirect:/feedback/list";
    }

    // ---- Read ----
    @GetMapping("/list")
    public String listFeedback(@RequestParam(required = false, defaultValue = "all") String filter,
                               HttpSession session, Model model) {
        Customer loggedInCustomer = (Customer) session.getAttribute("loggedInCustomer");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        Long loggedInCustomerId = loggedInCustomer != null ? loggedInCustomer.getCustomerId() : null;

        // "mine" only makes sense when someone is actually logged in - fall back to "all" otherwise
        if ("mine".equals(filter) && loggedInCustomer == null) {
            filter = "all";
        }

        // Strategy pattern: FeedbackFilterFactory hands back whichever FeedbackFilterStrategy
        // matches the requested code (all / mine / high / low) - this controller never has
        // to know the filtering rule itself, so adding a new filter later needs no change here.
        FeedbackFilterStrategy strategy = feedbackFilterFactory.getStrategy(filter);
        List<Feedback> feedbackList = strategy.filter(feedbackService.getAll(), loggedInCustomerId);
        filter = strategy.getCode();

        model.addAttribute("feedbackList", feedbackList);
        model.addAttribute("averageRating", feedbackService.getAverageRating());
        model.addAttribute("loggedInCustomerId", loggedInCustomer != null ? loggedInCustomer.getCustomerId() : null);
        model.addAttribute("filter", filter);
        // tabs only make sense for a logged-in customer, not for admin browsing feedback
        model.addAttribute("showTabs", loggedInCustomer != null && (isAdmin == null || !isAdmin));
        return "feedback/list";
    }

    // ---- Update ----
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model) {
        Feedback feedback = feedbackService.getById(id);
        Customer loggedInCustomer = (Customer) session.getAttribute("loggedInCustomer");
        if (loggedInCustomer == null || feedback.getCustomer() == null
                || !feedback.getCustomer().getCustomerId().equals(loggedInCustomer.getCustomerId())) {
            return "redirect:/feedback/list";
        }
        model.addAttribute("feedback", feedback);
        return "feedback/edit";
    }

    @PostMapping("/update")
    public String updateFeedback(@RequestParam Long feedbackId, @RequestParam(required = false) Integer rating,
                                 @RequestParam(required = false) String comment, HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Feedback feedback = feedbackService.getById(feedbackId);
        Customer loggedInCustomer = (Customer) session.getAttribute("loggedInCustomer");
        if (loggedInCustomer == null || feedback.getCustomer() == null
                || !feedback.getCustomer().getCustomerId().equals(loggedInCustomer.getCustomerId())) {
            return "redirect:/feedback/list";
        }

        if (rating == null || rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("error", "Please select a rating between 1 and 5.");
            return "redirect:/feedback/edit/" + feedbackId;
        }
        if (comment == null || comment.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Please write a short comment about your experience.");
            return "redirect:/feedback/edit/" + feedbackId;
        }

        feedbackService.update(feedbackId, rating, comment.trim());
        redirectAttributes.addFlashAttribute("success", "Your feedback has been updated.");
        return "redirect:/feedback/list";
    }

    // ---- Delete ----
    @GetMapping("/delete/{id}")
    public String deleteFeedback(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Feedback feedback = feedbackService.getById(id);
        Customer loggedInCustomer = (Customer) session.getAttribute("loggedInCustomer");
        if (loggedInCustomer != null && feedback.getCustomer() != null
                && feedback.getCustomer().getCustomerId().equals(loggedInCustomer.getCustomerId())) {
            feedbackService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Feedback deleted.");
        }
        return "redirect:/feedback/list";
    }
}

package com.foodorderingsystem.feedback.strategy;

import com.foodorderingsystem.feedback.entity.Feedback;
import org.springframework.stereotype.Component;

import java.util.List;

// Shows only the unhappy customers (1-2 stars) - useful for admin to see what needs fixing.
@Component
public class LowRatingFilterStrategy implements FeedbackFilterStrategy {

    public static final String CODE = "low";

    @Override
    public List<Feedback> filter(List<Feedback> allFeedback, Long loggedInCustomerId) {
        return allFeedback.stream().filter(f -> f.getRating() != null && f.getRating() <= 2).toList();
    }

    @Override
    public String getCode() {
        return CODE;
    }
}

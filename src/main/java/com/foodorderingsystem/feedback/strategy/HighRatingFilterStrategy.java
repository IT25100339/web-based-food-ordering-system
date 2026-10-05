package com.foodorderingsystem.feedback.strategy;

import com.foodorderingsystem.feedback.entity.Feedback;
import org.springframework.stereotype.Component;

import java.util.List;

// Shows only the happy customers (4-5 stars) - useful for admin to see what's working well.
@Component
public class HighRatingFilterStrategy implements FeedbackFilterStrategy {

    public static final String CODE = "high";

    @Override
    public List<Feedback> filter(List<Feedback> allFeedback, Long loggedInCustomerId) {
        return allFeedback.stream().filter(f -> f.getRating() != null && f.getRating() >= 4).toList();
    }

    @Override
    public String getCode() {
        return CODE;
    }
}

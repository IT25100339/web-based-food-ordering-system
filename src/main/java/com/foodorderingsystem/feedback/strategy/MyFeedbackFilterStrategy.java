package com.foodorderingsystem.feedback.strategy;

import com.foodorderingsystem.feedback.entity.Feedback;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyFeedbackFilterStrategy implements FeedbackFilterStrategy {

    public static final String CODE = "mine";

    @Override
    public List<Feedback> filter(List<Feedback> allFeedback, Long loggedInCustomerId) {
        if (loggedInCustomerId == null) return List.of();
        return allFeedback.stream()
                .filter(f -> f.getCustomer() != null && loggedInCustomerId.equals(f.getCustomer().getCustomerId()))
                .toList();
    }

    @Override
    public String getCode() {
        return CODE;
    }
}

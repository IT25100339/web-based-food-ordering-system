package com.foodorderingsystem.feedback.strategy;

import com.foodorderingsystem.feedback.entity.Feedback;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AllFeedbackFilterStrategy implements FeedbackFilterStrategy {

    public static final String CODE = "all";

    @Override
    public List<Feedback> filter(List<Feedback> allFeedback, Long loggedInCustomerId) {
        return allFeedback;
    }

    @Override
    public String getCode() {
        return CODE;
    }
}

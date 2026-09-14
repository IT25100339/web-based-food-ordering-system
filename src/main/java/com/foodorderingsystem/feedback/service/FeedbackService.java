package com.foodorderingsystem.feedback.service;

import com.foodorderingsystem.feedback.entity.Feedback;
import com.foodorderingsystem.feedback.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    public Feedback save(Feedback feedback) {
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getAll() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    public double getAverageRating() {
        List<Feedback> all = feedbackRepository.findAll();
        if (all.isEmpty()) return 0;
        return all.stream().mapToInt(Feedback::getRating).average().orElse(0);
    }
}


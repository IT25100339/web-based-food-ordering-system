package com.foodorderingsystem.feedback.strategy;

import com.foodorderingsystem.feedback.entity.Feedback;

import java.util.List;

// Strategy pattern: each concrete strategy defines its own rule for narrowing down
// the full feedback list. FeedbackController (the "client") doesn't need to know
// the filtering rule itself - it just asks FeedbackFilterFactory for the right
// strategy and calls filter() on it.
public interface FeedbackFilterStrategy {
    List<Feedback> filter(List<Feedback> allFeedback, Long loggedInCustomerId);

    // the value used in ?filter=... and to look the strategy up in the factory
    String getCode();
}

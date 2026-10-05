package com.foodorderingsystem.feedback.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Factory pattern paired with the Strategy pattern above: Spring injects every
// FeedbackFilterStrategy bean, and this factory indexes them by their code so
// FeedbackController can ask for "mine", "all", "high" or "low" without an if/else chain.
@Component
public class FeedbackFilterFactory {

    private final Map<String, FeedbackFilterStrategy> strategiesByCode = new HashMap<>();

    @Autowired
    public FeedbackFilterFactory(List<FeedbackFilterStrategy> strategies) {
        for (FeedbackFilterStrategy strategy : strategies) {
            strategiesByCode.put(strategy.getCode(), strategy);
        }
    }

    public FeedbackFilterStrategy getStrategy(String code) {
        return strategiesByCode.getOrDefault(code, strategiesByCode.get(AllFeedbackFilterStrategy.CODE));
    }
}


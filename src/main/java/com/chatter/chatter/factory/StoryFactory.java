package com.chatter.chatter.factory;

import com.chatter.chatter.creator.StoryCreator;
import com.chatter.chatter.model.StoryType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StoryFactory {

    private final List<StoryCreator> storyCreators;

    @Autowired
    public StoryFactory(List<StoryCreator> storyCreators) {
        this.storyCreators = storyCreators;
    }

    public StoryCreator getStoryCreator(StoryType storyType) {
        return storyCreators.stream()
                .filter(storyCreator -> storyCreator.supports(storyType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported story type"));
    }

}

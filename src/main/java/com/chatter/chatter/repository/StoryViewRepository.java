package com.chatter.chatter.repository;

import com.chatter.chatter.model.StoryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryViewRepository extends JpaRepository<StoryView, Long> {

    List<StoryView> findByStoryId(Long storyId);

    boolean existsByUserEmailAndStoryId(String email, Long storyId);

}
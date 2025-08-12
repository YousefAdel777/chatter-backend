package com.chatter.chatter.repository;

import com.chatter.chatter.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    void deleteByOptionPollMessageIdAndUserEmail(Long messageId, String email);

    boolean existsByUserEmailAndOptionId(String email, Long optionId);

    boolean existsByOptionPollMessageIdAndUserEmail(Long messageId, String email);

}

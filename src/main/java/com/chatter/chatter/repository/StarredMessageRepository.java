package com.chatter.chatter.repository;

import com.chatter.chatter.model.StarredMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StarredMessageRepository extends JpaRepository<StarredMessage, Long> {

    Page<StarredMessage> findByUserEmail(String email, Pageable pageable);

    Page<StarredMessage> findByUserEmailAndMessageChatId(String email, Long chatId, Pageable pageable);

    Optional<StarredMessage> findByUserEmailAndMessageId(String email, Long chatId);

    boolean existsByUserEmailAndMessageId(String email, Long messageId);

}

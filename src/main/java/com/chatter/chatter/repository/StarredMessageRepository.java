package com.chatter.chatter.repository;

import com.chatter.chatter.model.StarredMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StarredMessageRepository extends JpaRepository<StarredMessage, Long> {

    Page<StarredMessage> findByUserEmail(String email, Pageable pageable);

    Page<StarredMessage> findByUserEmailAndMessageChatId(String email, Long chatId, Pageable pageable);

    @Query("""
        SELECT sm FROM StarredMessage sm
        WHERE sm.user.email = :email AND sm.message.id = :messageId
        AND EXISTS (
            SELECT 1 FROM Member mem
            WHERE mem.user.email = :email AND mem.chat = sm.message.chat
        )
    """)
    Optional<StarredMessage> findByUserEmailAndMessageId(String email, Long messageId);

    boolean existsByUserEmailAndMessageId(String email, Long messageId);

}

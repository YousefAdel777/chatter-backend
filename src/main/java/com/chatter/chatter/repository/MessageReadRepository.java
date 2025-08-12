package com.chatter.chatter.repository;

import com.chatter.chatter.model.MessageRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageReadRepository extends JpaRepository<MessageRead, Long> {

    boolean existsByUserEmailAndMessageId(String email, Long messageId);

    List<MessageRead> findByMessageId(Long messageId);

}

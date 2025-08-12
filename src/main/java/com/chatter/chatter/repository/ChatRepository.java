package com.chatter.chatter.repository;

import com.chatter.chatter.model.Chat;
import com.chatter.chatter.model.ChatType;
import com.chatter.chatter.model.User;
import org.hibernate.query.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long>, JpaSpecificationExecutor<Chat> {

    @Query("""
        SELECT c FROM Chat c
        WHERE :size = (SELECT COUNT(m) FROM c.members m)
        AND c.chatType = :chatType
        AND NOT EXISTS (
            SELECT m FROM c.members m
            WHERE m.user.id NOT IN :userIds
        )
    """)
    Optional<Chat> findByUsers(@Param("size") Long size, @Param("chatType") ChatType chatType, @Param("userIds") Set<Long> userIds);

    List<Chat> findAll(Specification<Chat> specification);

    @Query("""
        SELECT c FROM Chat c
        WHERE c.id IN :chatIds
        AND EXISTS (
            SELECT 1 FROM Member m
            WHERE m.user.email = :email
            AND m.chat = c
        )
    """)
    List<Chat> findChatsByIds(@Param("email") String email, @Param("chatIds") Iterable<Long> chatIds);

//    @Query("""
//        SELECT c FROM Chat c
//        WHERE c.id = :chatId
//        AND EXISTS (
//            SELECT 1 FROM Member mem
//            WHERE mem.user.email = :email
//            AND mem.chat.id = :chatId
//        )
//    """)
    @Query("""
        SELECT c FROM Chat c
        WHERE c.id = :chatId
    """)
    Optional<Chat> findChatById(@Param("email") String email, @Param("chatId") Long chatId);

    @Query("SELECT c FROM Chat c JOIN c.members m WHERE m.user.email = :email AND c.id = :chatId")
    Optional<Chat> findByIdAndMemberEmail(@Param("email") String email, @Param("chatId") Long chatId);
}

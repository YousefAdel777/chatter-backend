//package com.chatter.chatter.repository;
//
//import com.chatter.chatter.model.Chat;
//import com.chatter.chatter.model.Message;
//import jakarta.validation.constraints.NotNull;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.data.jpa.repository.EntityGraph;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//
//@Repository
//public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {
//
//    Optional<Message> findByIdAndUserEmail(Long id, String email);
//
//    Optional<Message> findByIdAndChat(Long id, Chat chat);
//
//    @Query("""
//            SELECT m FROM Message m
//            WHERE m.chat.id = :chatId AND m.user.email != :email
//            AND EXISTS (
//                SELECT 1 FROM Member mem
//                WHERE mem.chat.id = :chatId
//                AND mem.user.email = :email
//            )
//            AND NOT EXISTS (
//                SELECT 1 FROM MessageRead r
//                WHERE r.message = m
//                AND r.user.email = :email
//            )
//    """)
//    List<Message> findUnreadMessages(@Param("email") String email, @Param("chatId") Long chatId);
//
//    @Query("""
//        SELECT DISTINCT m FROM Message m
//        LEFT JOIN m.user u
//        WHERE m.id IN :messagesIds
//        AND (u IS NULL OR u.email != :email)
//        AND EXISTS (
//            SELECT 1 FROM Member mem
//            WHERE mem.chat = m.chat
//            AND mem.user.email = :email
//        )
//        AND NOT EXISTS (
//            SELECT 1 FROM MessageRead r
//            WHERE r.message = m
//            AND r.user.email = :email
//        )
//    """)
//    List<Message> findUnreadMessagesByIds(@Param("email") String email, @Param("messagesIds") Iterable<Long> messagesIds);
//
//}


package com.chatter.chatter.repository;

import com.chatter.chatter.dto.MessageProjection;
import com.chatter.chatter.dto.MessageStatusProjection;
import com.chatter.chatter.model.Chat;
import com.chatter.chatter.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {

    Optional<Message> findByIdAndUserEmail(Long id, String email);

    Optional<Message> findByIdAndChat(Long id, Chat chat);
//
//    @Query("""
//        SELECT
//        DISTINCT
//        m as message,
//        (sm.id IS NOT NULL) as isStarred,
//        (mr.id IS NOT NULL) as isSeen
//        FROM Message m
//        LEFT JOIN MessageRead mr ON mr.message = m AND mr.user.email = :email
//        LEFT JOIN StarredMessage sm ON sm.message = m AND sm.user.email = :email
//        LEFT JOIN FETCH m.replyMessage
//        LEFT JOIN FETCH m.reacts
//        LEFT JOIN FETCH m.user
//        LEFT JOIN FETCH TREAT(m as InviteMessage).invite
//        LEFT JOIN FETCH TREAT(m as StoryMessage).story
//        LEFT JOIN FETCH TREAT(m as PollMessage).options
//        LEFT JOIN FETCH TREAT(m as MediaMessage).attachments
//        WHERE m.id in :messagesIds
//    """)

    @Query("""
        SELECT
            new com.chatter.chatter.dto.MessageStatusProjection(
                m.id,
                (sm.id IS NOT NULL),
                (mr.id IS NOT NULL)
            )
        FROM Message m
        LEFT JOIN MessageRead mr ON mr.message = m AND mr.user.email = :email
        LEFT JOIN StarredMessage sm ON sm.message = m AND sm.user.email = :email
        WHERE m.id in :messagesIds
    """)
    List<MessageStatusProjection> findMessageStatus(@Param("email") String email, @Param("messagesIds") Iterable<Long> messagesIds);

    @Query("""
            SELECT m FROM Message m
            WHERE m.chat.id = :chatId AND m.user.email != :email
            AND EXISTS (
                SELECT 1 FROM Member mem
                WHERE mem.chat.id = :chatId
                AND mem.user.email = :email
            )
            AND NOT EXISTS (
                SELECT 1 FROM MessageRead r
                WHERE r.message = m
                AND r.user.email = :email
            )
    """)
    List<Message> findUnreadMessages(@Param("email") String email, @Param("chatId") Long chatId);

    @EntityGraph(value = "graph.messages")
    Page<Message> findAll(Specification<Message> spec, Pageable pageable);

    @Query("""
        SELECT DISTINCT m FROM Message m
        LEFT JOIN m.user u
        WHERE m.id IN :messagesIds
        AND (u IS NULL OR u.email != :email)
        AND EXISTS (
            SELECT 1 FROM Member mem
            WHERE mem.chat = m.chat
            AND mem.user.email = :email
        )
        AND NOT EXISTS (
            SELECT 1 FROM MessageRead r
            WHERE r.message = m
            AND r.user.email = :email
        )
    """)
    List<Message> findUnreadMessagesByIds(@Param("email") String email, @Param("messagesIds") Iterable<Long> messagesIds);

}

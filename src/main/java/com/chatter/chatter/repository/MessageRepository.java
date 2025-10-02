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

    @Query("""
        SELECT
            new com.chatter.chatter.dto.MessageStatusProjection(
                m.id,
                u.id,
                u.email,
                CASE WHEN EXISTS (
                        SELECT 1 FROM MessageRead mr
                        WHERE mr.message = m
                        AND (mr.user = u OR mr.showRead IS TRUE)
                ) THEN TRUE ELSE FALSE END,
                CASE WHEN EXISTS(
                    SELECT 1 FROM StarredMessage sm
                    WHERE sm.message = m AND sm.user = u
                ) THEN TRUE ELSE FALSE END
            )
            FROM Message m, User u
            WHERE m.id IN :messagesIds
            AND u.email IN :emails
    """)
    List<MessageStatusProjection> findMessageStatus(@Param("emails") Iterable<String> emails, @Param("messagesIds") Iterable<Long> messagesIds);

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

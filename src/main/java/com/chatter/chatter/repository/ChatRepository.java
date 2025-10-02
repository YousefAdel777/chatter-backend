package com.chatter.chatter.repository;

import com.chatter.chatter.dto.ChatStatusProjection;
import com.chatter.chatter.model.Chat;
import com.chatter.chatter.model.ChatType;
import org.springframework.data.jpa.domain.Specification;
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
    Optional<Chat> findByUsers(@Param("size") Long size, @Param("chatType") ChatType chatType, @Param("userIds") Iterable<Long> userIds);

    List<Chat> findAll(Specification<Chat> specification);

        @Query("""
            SELECT
                new com.chatter.chatter.dto.ChatStatusProjection(
                c.id,
                u.id,
                u.email,
                (SELECT COUNT(msg) FROM Message msg
                 WHERE msg.chat = c
                 AND msg.user != u
                 AND NOT EXISTS (
                     SELECT 1 FROM MessageRead mr
                     WHERE mr.message = msg AND mr.user = u
                 )),
                CAST((SELECT COUNT(mem) FROM c.members mem) as LONG),
                (SELECT MIN(msg.id) FROM Message msg
                 WHERE msg.chat = c
                 AND msg.user != u
                 AND NOT EXISTS (
                     SELECT 1 FROM MessageRead mr
                     WHERE mr.message = msg AND mr.user = u
                 )),
                CASE WHEN EXISTS(SELECT 1 FROM Mention m
                 WHERE m.message.chat = c
                 AND m.message.user != u
                 AND (m.user = u OR m.message.isEveryoneMentioned)
                 AND NOT EXISTS (
                     SELECT 1 FROM MessageRead mr
                     WHERE mr.message = m.message AND mr.user = u
                 )) THEN TRUE ELSE FALSE END
            )
            FROM Chat c, User u
            WHERE c.id IN :chatIds
            AND u.email IN :emails
    """)
    List<ChatStatusProjection> findChatStatus(@Param("emails") Set<String> emails, @Param("chatIds") Iterable<Long> chatIds);

//    @Query("""
//        SELECT c.id,
//            c.chatType,
//            c.createdAt,
//            CASE WHEN c.chatType = 'INDIVIDUAL'
//                 THEN (SELECT u.id FROM c.members mem JOIN mem.user u WHERE u.email != :email)
//                 ELSE NULL END,
//            CASE WHEN c.chatType = 'INDIVIDUAL'
//                 THEN (SELECT u.username FROM c.members mem JOIN mem.user u WHERE u.email != :email)
//                 ELSE NULL END,
//            CASE WHEN c.chatType = 'INDIVIDUAL'
//                 THEN (SELECT u.email FROM c.members mem JOIN mem.user u WHERE u.email != :email)
//                 ELSE NULL END,
//            CASE WHEN c.chatType = 'INDIVIDUAL'
//                 THEN (SELECT u.createdAt FROM c.members mem JOIN mem.user u WHERE u.email != :email)
//                 ELSE NULL END,
//            CASE WHEN c.chatType = 'INDIVIDUAL'
//                 THEN (SELECT u.image FROM c.members mem JOIN mem.user u WHERE u.email != :email)
//                 ELSE NULL END,
//            CASE WHEN c.chatType = 'INDIVIDUAL'
//                 THEN (SELECT u.bio FROM c.members mem JOIN mem.user u WHERE u.email != :email)
//                 ELSE NULL END,
//            CASE WHEN c.chatType = 'INDIVIDUAL'
//                 THEN (SELECT u.lastOnline FROM c.members mem JOIN mem.user u WHERE u.email != :email)
//                 ELSE NULL END,
//            CASE WHEN c.chatType = 'INDIVIDUAL'
//                 THEN (SELECT u.showOnlineStatus FROM c.members mem JOIN mem.user u WHERE u.email != :email)
//                 ELSE NULL END,
//            CASE WHEN c.chatType = 'INDIVIDUAL'
//                 THEN (SELECT u.showMessageReads FROM c.members mem JOIN mem.user u WHERE u.email != :email)
//                 ELSE NULL END,
//            (SELECT m.id FROM Message m WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT m.messageType FROM Message m WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT m.content FROM Message m WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT m.createdAt FROM Message m WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT mu.id FROM Message m JOIN m.user mu WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT mu.username FROM Message m JOIN m.user mu WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT mu.email FROM Message m JOIN m.user mu WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT mu.createdAt FROM Message m JOIN m.user mu WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT mu.image FROM Message m JOIN m.user mu WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT mu.bio FROM Message m JOIN m.user mu WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT mu.lastOnline FROM Message m JOIN m.user mu WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT mu.showOnlineStatus FROM Message m JOIN m.user mu WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT mu.showMessageReads FROM Message m JOIN m.user mu WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT p.title FROM Message m LEFT JOIN PollMessage p ON p.id = m.id WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT cm.isMissed FROM Message m LEFT JOIN CallMessage cm ON cm.id = m.id WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT cm.duration FROM Message m LEFT JOIN CallMessage cm ON cm.id = m.id WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT fm.originalFileName FROM Message m LEFT JOIN FileMessage fm ON fm.id = m.id WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT SIZE(mm.attachments) FROM Message m LEFT JOIN MediaMessage mm ON mm.id = m.id WHERE m.chat = c
//             AND m.createdAt = (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c)),
//            (SELECT COUNT(msg) FROM Message msg WHERE msg.chat = c AND msg.user.email != :email AND NOT EXISTS (
//                SELECT 1 FROM MessageRead mr
//                WHERE mr.user.email = :email AND mr.message = msg
//            )),
//            (SELECT COUNT(mem) FROM c.members mem),
//            (SELECT MIN(msg.id) FROM Message msg WHERE msg.chat = c AND msg.user.email != :email AND NOT EXISTS (
//                SELECT 1 FROM MessageRead mr
//                WHERE mr.user.email = :email AND mr.message = msg
//            )),
//            EXISTS (SELECT 1 FROM Message msg WHERE msg.chat = c AND msg.user.email != :email AND NOT EXISTS (
//                SELECT 1 FROM MessageRead mr
//                WHERE mr.user.email = :email AND mr.message = msg
//            ) AND (msg.isEveryoneMentioned OR EXISTS (SELECT 1 FROM Mention m WHERE m.message = msg AND m.user.email = :email))),
//            g.name,
//            g.description,
//            g.image,
//            g.onlyAdminsCanSend,
//            g.onlyAdminsCanInvite,
//            g.onlyAdminsCanEditGroup,
//            g.onlyAdminsCanPin
//        FROM Chat c
//        LEFT JOIN GroupChat g ON g.id = c.id
//        WHERE c.id IN :chatIds
//        AND EXISTS (SELECT 1 FROM c.members mem WHERE mem.user.email = :email)
//        ORDER BY (SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.chat = c) DESC NULLS LAST
//    """)
//    List<Object[]> findChatDtosByIds(@Param("email") String email, @Param("chatIds") Iterable<Long> chatIds);

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

    @Query("""
        SELECT c FROM Chat c
        WHERE c.id = :chatId
        AND EXISTS (
            SELECT 1 FROM Member mem
            WHERE mem.user.email = :email
            AND mem.chat.id = :chatId
        )
    """)
    Optional<Chat> findChatById(@Param("email") String email, @Param("chatId") Long chatId);

}

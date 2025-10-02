package com.chatter.chatter.repository;

import com.chatter.chatter.model.ChatType;
import com.chatter.chatter.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("""
           SELECT DISTINCT u FROM User u
           JOIN Member m ON m.user = u
           JOIN Chat c ON m.chat = c
           WHERE c.chatType = :chatType
           AND EXISTS (
                SELECT 1 FROM Member m2 WHERE
                m2.user.email = :email
                AND m2.chat = c
           )
           AND u.email != :email
    """)
    List<User> findContacts(@Param("email") String email, @Param("chatType") ChatType chatType);

    @Query("""
        SELECT u FROM User u
        WHERE u.id in :usersIds
        AND EXIStS (
            SELECT 1 FROM Member m
            WHERE m.user = u AND m.chat.id = :chatId
        )
    """)
    List<User> findUsersByIdInAndChatMembership(@Param("usersIds") Set<Long> userIds, @Param("chatId") Long chatId);

}
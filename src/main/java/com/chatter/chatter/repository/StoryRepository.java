package com.chatter.chatter.repository;

import com.chatter.chatter.model.ChatType;
import com.chatter.chatter.model.Story;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    @Query("""
        SELECT DISTINCT s FROM Story s
        JOIN Member m ON m.user.email = :email
        JOIN m.chat c ON c.chatType = :chatType
        JOIN Member m2 ON m2.chat = c AND m2.user = s.user
        LEFT JOIN Block b ON (
            (b.blockedBy.email = :email AND b.blockedUser = s.user)
            OR (b.blockedBy = s.user AND b.blockedUser.email = :email)
        )
        WHERE s.user.email != :email
        AND b.id IS NULL
        AND s.createdAt >= :cutoff
        AND :email NOT IN (SELECT u.email FROM s.excludedUsers u)
    """)
    List<Story> findStories(@Param("email") String email, @Param("chatType") ChatType chatType, @Param("cutoff") Instant cutoff);

    @Query("""
        SELECT DISTINCT s FROM Story s
        LEFT JOIN Member m ON m.user.email = :email
        LEFT JOIN m.chat c ON c.chatType = :chatType
        LEFT JOIN Member m2 ON m2.chat = c AND m2.user = s.user
        LEFT JOIN Block b ON (
            (b.blockedBy.email = :email AND b.blockedUser = s.user)
            OR (b.blockedBy = s.user AND b.blockedUser.email = :email)
        )
        WHERE s.id = :id
        AND b.id IS NULL
        AND s.createdAt >= :cutoff
        AND (
            s.user.email = :email
            OR m2.id IS NOT NULL
        )
        AND :email NOT IN (SELECT u.email FROM s.excludedUsers u)
    """)
    Optional<Story> findStoryById(@Param("email") String email, @Param("chatType") ChatType chatType, @Param("cutoff") Instant cutoff, @Param("id") Long id);

    @EntityGraph(attributePaths = { "user" })
    List<Story> findStoriesByUserEmail(String email);

    void deleteByCreatedAtBefore(Instant date);

    @EntityGraph(attributePaths = { "user" })
    List<Story> findByCreatedAtBefore(Instant data);

}

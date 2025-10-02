//package com.chatter.chatter.repository;
//
//import com.chatter.chatter.model.ChatType;
//import com.chatter.chatter.model.Story;
//import org.springframework.data.jpa.repository.EntityGraph;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface StoryRepository extends JpaRepository<Story, Long> {
//
//    @Query("""
//        SELECT DISTINCT s FROM Story s
//        JOIN Member m ON m.user.email = :email
//        JOIN m.chat c ON c.chatType = :chatType
//        JOIN Member m2 ON m2.chat = c AND m2.user = s.user
//        LEFT JOIN Block b ON (
//            (b.blockedBy.email = :email AND b.blockedUser = s.user)
//            OR (b.blockedBy = s.user AND b.blockedUser.email = :email)
//        )
//        WHERE s.user.email != :email
//        AND b.id IS NULL
//        AND s.createdAt >= :cutoff
//        AND :email NOT IN (SELECT u.email FROM s.excludedUsers u)
//    """)
//    List<Story> findStories(@Param("email") String email, @Param("chatType") ChatType chatType, @Param("cutoff") Instant cutoff);
//
//    @Query("""
//        SELECT DISTINCT s FROM Story s
//        LEFT JOIN Member m ON m.user.email = :email
//        LEFT JOIN m.chat c ON c.chatType = :chatType
//        LEFT JOIN Member m2 ON m2.chat = c AND m2.user = s.user
//        LEFT JOIN Block b ON (
//            (b.blockedBy.email = :email AND b.blockedUser = s.user)
//            OR (b.blockedBy = s.user AND b.blockedUser.email = :email)
//        )
//        WHERE s.id = :id
//        AND b.id IS NULL
//        AND s.createdAt >= :cutoff
//        AND (
//            s.user.email = :email
//            OR m2.id IS NOT NULL
//        )
//        AND :email NOT IN (SELECT u.email FROM s.excludedUsers u)
//    """)
//    Optional<Story> findStoryById(@Param("email") String email, @Param("chatType") ChatType chatType, @Param("cutoff") Instant cutoff, @Param("id") Long id);
//
//    @EntityGraph(attributePaths = { "user" })
//    List<Story> findStoriesByUserEmail(String email);
//
//    @EntityGraph(attributePaths = { "user" })
//    List<Story> findByCreatedAtBefore(Instant data);
//
//}

package com.chatter.chatter.repository;

import com.chatter.chatter.dto.StoryStatusProjection;
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

//    @Query("""
//        SELECT DISTINCT
//            s.id as id,
//            s.content as content,
//            COALESCE(ts.textColor, '') as textColor,
//            COALESCE(ts.backgroundColor, '') as backgroundColor,
//            COALESCE(ms.filePath, '') as filePath,
//            s.createdAt as createdAt,
//            s.storyType as storyType,
//            s.user as user,
//            eu as excludedUsers,
//            (sv.id IS NOT NULL) as isViewed
//        FROM Story s
//        LEFT JOIN TextStory ts ON ts.id = s.id
//        LEFT JOIN MediaStory ms ON ms.id = s.id
//        JOIN Member m ON m.user.email = :email
//        JOIN m.chat c ON c.chatType = :chatType
//        JOIN Member m2 ON m2.chat = c AND m2.user = s.user
//        LEFT JOIN Block b ON (
//            (b.blockedBy.email = :email AND b.blockedUser = s.user)
//            OR (b.blockedBy = s.user AND b.blockedUser.email = :email)
//        )
//        LEFT JOIN StoryView sv ON sv.story = s AND sv.user.email = :email
//        LEFT JOIN s.excludedUsers eu
//        WHERE s.user.email != :email
//        AND b.id IS NULL
//        AND s.createdAt >= :cutoff
//        AND :email NOT IN (SELECT u.email FROM s.excludedUsers u)
//    """)
//    List<StoryProjection> findStories(@Param("email") String email, @Param("chatType") ChatType chatType, @Param("cutoff") Instant cutoff);
//
//    @Query("""
//        SELECT
//            s.id as id,
//            s.content as content,
//            COALESCE(ts.textColor, '') as textColor,
//            COALESCE(ts.backgroundColor, '') as backgroundColor,
//            COALESCE(ms.filePath, '') as filePath,
//            s.createdAt as createdAt,
//            s.storyType as storyType,
//            s.user as user,
//            eu as excludedUsers,
//            (sv.id IS NOT NULL) as isViewed
//        FROM Story s
//        LEFT JOIN TextStory ts ON ts.id = s.id
//        LEFT JOIN MediaStory ms ON ms.id = s.id
//        JOIN Member m ON m.user.email = :email
//        JOIN m.chat c ON c.chatType = :chatType
//        JOIN Member m2 ON m2.chat = c AND m2.user = s.user
//        LEFT JOIN Block b ON (
//            (b.blockedBy.email = :email AND b.blockedUser = s.user)
//            OR (b.blockedBy = s.user AND b.blockedUser.email = :email)
//        )
//        LEFT JOIN StoryView sv ON sv.story = s AND sv.user.email = :email
//        LEFT JOIN s.excludedUsers eu
//        WHERE s.id = :id
//        AND b.id IS NULL
//        AND s.createdAt >= :cutoff
//        AND (
//            s.user.email = :email
//            OR m2.id IS NOT NULL
//        )
//        AND :email NOT IN (SELECT u.email FROM s.excludedUsers u)
//    """)
//    Optional<StoryProjection> findStoryProjectionById(@Param("email") String email, @Param("chatType") ChatType chatType, @Param("cutoff") Instant cutoff, @Param("id") Long id);

    @Query("""
        SELECT new com.chatter.chatter.dto.StoryStatusProjection(
            s.id,
            CASE WHEN EXISTS(
               SELECT 1 FROM StoryView sv
               WHERE sv.story = s AND sv.user.email = :email
            ) THEN TRUE ELSE FALSE END
        )
        FROM Story s
    """)
    List<StoryStatusProjection> findStoryStatus(@Param("email") String email, @Param("") Iterable<Long> storiesIds);

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

    @Query("""
        SELECT s FROM Story s
            WHERE s.user.email != :email
            AND NOT EXISTS (
                SELECT 1 FROM Block b
                WHERE (b.blockedBy.email = :email AND b.blockedUser = s.user)
                OR (b.blockedBy = s.user AND b.blockedUser.email = :email)
            )
            AND EXISTS (
                SELECT 1 FROM Member m1
                JOIN Member m2 ON m2.chat = m1.chat
                WHERE m1.user.email = :email
                AND m1.chat.chatType = :chatType
                AND m2.user = s.user
            )
            AND s.createdAt >= :cutoff
            AND :email NOT IN (SELECT u.email FROM s.excludedUsers u)
    """)
    List<Story> findStories(@Param("email") String email, @Param("chatType") ChatType chatType, @Param("cutoff") Instant cutoff);

    @EntityGraph(attributePaths = { "user", "excludedUsers" })
    List<Story> findStoriesByUserEmail(String email);

    @EntityGraph(attributePaths = { "user", "excludedUsers" })
    List<Story> findByCreatedAtBefore(Instant data);
}

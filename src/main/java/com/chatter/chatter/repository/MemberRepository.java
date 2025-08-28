package com.chatter.chatter.repository;

import com.chatter.chatter.model.Member;
import com.chatter.chatter.model.MemberRole;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {

    @EntityGraph(attributePaths = { "chat", "user" })
    Optional<Member> findByChatIdAndUserEmail(Long chatId, String email);

    boolean existsByChatIdAndUserEmail(Long chatId, String email);

    boolean existsByChatIdAndUserEmailAndMemberRole(Long chatId, String email, MemberRole role);

    boolean existsByChatIdAndUserId(Long chatId, Long userId);

    @Query("""
        SELECT m FROM Member m
        WHERE m.chat.id = :chatId
        AND m.memberRole = :role
        AND m.id != :memberId
        ORDER BY m.id ASC
        LIMIT 1
    """)
    Optional<Member> findFirstMemberExcludingMember(@Param("chatId") Long chatId,@Param("memberId") Long memberId, @Param("role") MemberRole role);

}
package com.chatter.chatter.repository;

import com.chatter.chatter.model.Member;
import com.chatter.chatter.model.MemberRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {

    @EntityGraph(attributePaths = { "chat", "user" })
    Optional<Member> findByChatIdAndUserEmail(Long chatId, String email);

    boolean existsByChatIdAndUserEmail(Long chatId, String email);

    boolean existsByChatIdAndUserEmailAndMemberRole(Long chatId, String email, MemberRole role);

    boolean existsByChatIdAndUserId(Long chatId, Long userId);

}
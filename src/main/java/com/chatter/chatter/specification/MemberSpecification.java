package com.chatter.chatter.specification;

import com.chatter.chatter.model.Chat;
import com.chatter.chatter.model.Member;
import com.chatter.chatter.model.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class MemberSpecification {

    public static Specification<Member> hasEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null) {
                return criteriaBuilder.isTrue(criteriaBuilder.literal(true));
            }
            Join<Member, User> userJoin = root.join("user");
            return criteriaBuilder.like(userJoin.get("email"), "%" + email + "%");
        };
    }

    public static Specification<Member> hasUsername(String username) {
        return (root, query, criteriaBuilder) -> {
            if (username == null) {
                return criteriaBuilder.isTrue(criteriaBuilder.literal(true));
            }
            Join<Member, User> userJoin = root.join("user");
            return criteriaBuilder.like(userJoin.get("username"), "%" + username + "%");
        };
    }

    public static Specification<Member> hasChatId(Long chatId) {
        return (root, query, criteriaBuilder) -> {
            if (chatId == null) {
                return criteriaBuilder.isTrue(criteriaBuilder.literal(true));
            }
            Join<Member, Chat> chatJoin = root.join("chat");
            return criteriaBuilder.equal(chatJoin.get("id"), chatId);
        };
    }

    public static Specification<Member> withFilters(Long chatId, String username, String email) {
        return Specification
                .where(hasChatId(chatId))
                .and(hasEmail(email).or(hasUsername(username)));
    }

}

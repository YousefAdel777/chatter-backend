package com.chatter.chatter.specification;

import com.chatter.chatter.model.Chat;
import com.chatter.chatter.model.ChatType;
import com.chatter.chatter.model.Member;
import com.chatter.chatter.model.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

public class ChatSpecification {

    public static Specification<Chat> hasName(String name, String email) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            query.distinct(true);

            Join<Chat, Member> chatMemberJoin = root.join("members");
            Join<Member, User> userJoin = chatMemberJoin.join("user");

            Predicate nameFilter = criteriaBuilder.like(root.get("name"), "%" + name + "%");
            Predicate excludeCurrentUser = criteriaBuilder.notLike(userJoin.get("username"), email);
            Predicate usernameFilter = criteriaBuilder.like(userJoin.get("username"), "%" + name + "%");
            Predicate chatTypeFilter = criteriaBuilder.equal(root.get("chatType"), ChatType.INDIVIDUAL);
            return criteriaBuilder.and(excludeCurrentUser, criteriaBuilder.or(nameFilter, criteriaBuilder.and(chatTypeFilter, usernameFilter)));
        };
    }

    public static Specification<Chat> hasMemberWithEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Member> memberRoot = subquery.from(Member.class);
            Join<Member, User> userJoin = memberRoot.join("user");
            subquery.select(criteriaBuilder.literal(1L))
                .where(
                    criteriaBuilder.and(
                            criteriaBuilder.equal(memberRoot.get("chat"), root),
                            criteriaBuilder.equal(userJoin.get("email"), email)
                    )
                );
            return criteriaBuilder.exists(subquery);
        };
    }

    public static Specification<Chat> hasDescription(String description) {
        return (root, query, criteriaBuilder) -> {
            if (description == null || description.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Predicate isGroupChat = criteriaBuilder.equal(root.get("chatType"), ChatType.GROUP);
            Predicate descriptionFilter = criteriaBuilder.like(root.get("description"), "%" + description + "%");
            return criteriaBuilder.and(isGroupChat, descriptionFilter);
        };
    }

    public static Specification<Chat> withFilters(String userEmail, String name, String description) {
        return Specification
                .where(hasMemberWithEmail(userEmail)
                .and(hasName(name, userEmail).or(hasDescription(description))));
    }

}

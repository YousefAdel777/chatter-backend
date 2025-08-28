package com.chatter.chatter.specification;

import com.chatter.chatter.model.Chat;
import com.chatter.chatter.model.ChatType;
import com.chatter.chatter.model.Member;
import com.chatter.chatter.model.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

public class ChatSpecification {

    public static Specification<Chat> hasName(String name, String email) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) {
                return cb.conjunction();
            }
            query.distinct(true);

            Join<Chat, Member> chatMemberJoin = root.join("members");
            Join<Member, User> userJoin = chatMemberJoin.join("user");

            Predicate nameFilter = cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");

            Predicate individualChatUsernameFilter = cb.and(
                    cb.equal(root.get("chatType"), ChatType.INDIVIDUAL),
                    cb.notEqual(userJoin.get("email"), email),
                    cb.like(cb.lower(userJoin.get("username")), "%" + name.toLowerCase() + "%")
            );

            return cb.or(nameFilter, individualChatUsernameFilter);
        };
    }

    public static Specification<Chat> hasMemberWithEmail(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isEmpty()) {
                return cb.conjunction();
            }
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Member> memberRoot = subquery.from(Member.class);
            Join<Member, User> userJoin = memberRoot.join("user");

            subquery.select(memberRoot.get("id"))
                    .where(
                            cb.and(
                                    cb.equal(memberRoot.get("chat"), root),
                                    cb.equal(userJoin.get("email"), email)
                            )
                    );
            return cb.exists(subquery);
        };
    }

    public static Specification<Chat> hasDescription(String description) {
        return (root, query, cb) -> {
            if (description == null || description.isEmpty()) {
                return cb.conjunction();
            }
            Predicate isGroupChat = cb.equal(root.get("chatType"), ChatType.GROUP);
            Predicate descriptionFilter = cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
            return cb.and(isGroupChat, descriptionFilter);
        };
    }

    public static Specification<Chat> withFilters(String userEmail, String name, String description) {
        return Specification
                .where(hasMemberWithEmail(userEmail))
                .and(hasName(name, userEmail).or(hasDescription(description)));
    }
}

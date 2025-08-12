package com.chatter.chatter.specification;

import com.chatter.chatter.model.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class MessageSpecification {

    public static Specification<Message> hasEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            Join<Message, Chat> chat = root.join("chat");
            Join<Chat, Member> member = chat.join("members");
            Join<Member, User> user = member.join("user");
            return criteriaBuilder.equal(user.get("email"), email);
        };
    }

    public static Specification<Message> hasContent(String content) {
        return (root, query, criteriaBuilder) -> {
            if (content == null || content.isBlank()) {
                return criteriaBuilder.isTrue(criteriaBuilder.literal(true));
            }
            return criteriaBuilder.like(root.get("content"), "%" + content + "%");
        };
    }

    public static Specification<Message> hasMessageType(MessageType messageType) {
        return (root, query, criteriaBuilder) -> {
            if (messageType == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("messageType"), messageType);
        };
    }

    public static Specification<Message> isPinned(Boolean pinned) {
        return (root, query, criteriaBuilder) -> {
            if (pinned == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("pinned"), pinned);
        };
    }

    public static Specification<Message> isStarred(Boolean starred, String email) {
        return (root, query, criteriaBuilder) -> {
            if (starred == null) {
                return criteriaBuilder.conjunction();
            }
            Join<Message, StarredMessage> starredMessages = root.join("starredMessages");
            Join<StarredMessage, User> user = starredMessages.join("user");
            Predicate match = criteriaBuilder.equal(user.get("email"), email);
            if (starred) {
                return match;
            }
            return criteriaBuilder.not(match);
        };
    }

    public static Specification<Message> hasChatId(Long chatId) {
        return (root, query, criteriaBuilder) -> {
            if (chatId == null) {
                return criteriaBuilder.conjunction();
            }
            Join<Message, Chat> chat = root.join("chat");
            return criteriaBuilder.equal(chat.get("id"), chatId);
        };
    }

    public static Specification<Message> withBefore(Long before) {
        return (root, query, criteriaBuilder) -> {
            if (before == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThan(root.get("id"), before);
        };
    }

    public static Specification<Message> withAfter(Long after) {
        return (root, query, criteriaBuilder) -> {
            if (after == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThan(root.get("id"), after);
        };
    }

    public static Specification<Message> withFilters(
            Long chatId,
            String content,
            MessageType messageType,
            Boolean pinned,
            Boolean starred,
            String email,
            Long before,
            Long after
    ) {
        return Specification
                .where(hasChatId(chatId))
                .and(hasEmail(email))
                .and(hasContent(content))
                .and(isPinned(pinned))
                .and(isStarred(starred, email))
                .and(hasMessageType(messageType))
                .and(withBefore(before))
                .and(withAfter(after));
    }

}

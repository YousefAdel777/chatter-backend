package com.chatter.chatter.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.*;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "chat")
@EntityListeners(AuditingEntityListener.class)
@Inheritance(strategy = InheritanceType.JOINED)
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Long id;

    @Builder.Default
    @OneToMany(mappedBy = "chat", orphanRemoval = true, cascade = CascadeType.ALL)
    @OrderBy("joinedAt ASC")
    private Set<Member> members = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "chat", orphanRemoval = true, cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    private List<Message> messages = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ChatType chatType = ChatType.INDIVIDUAL;

    @Column(updatable = false, nullable = false)
    @CreatedDate
    private Instant createdAt;

    public Long getUnreadMessagesCount(String email) {
        return messages.stream().filter(message -> !message.isSeen(email) && (message.getUser() == null || !email.equals(message.getUser().getEmail()))).count();
    }

    public Long getFirstUnreadMessageId(String email) {
        Message first = messages.stream().filter(message -> !message.isSeen(email) && (message.getUser() == null  || !email.equals(message.getUser().getEmail()))).findFirst().orElse(null);
        if (first == null) return null;
        return first.getId();
    }

    public Message getLastMessage() {
        if (messages.isEmpty()) return null;
        return messages.getLast();
    }

    public User getOtherUser(String email) {
        if (chatType.equals(ChatType.GROUP)) return null;
        for (Member member : members) {
            if (!member.getUser().getEmail().equals(email)) {
                return member.getUser();
            }
        }
        return null;
    }

    public void addMember(Member member) {
        members.add(member);
    }

    public void addMessage(Message message) {
        messages.add(message);
    }

}
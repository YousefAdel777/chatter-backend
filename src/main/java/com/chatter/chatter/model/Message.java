package com.chatter.chatter.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.Instant;
import java.util.*;

@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "messages")
@Inheritance(strategy = InheritanceType.JOINED)
@Entity
@NamedEntityGraph(
        name = "graph.messages",
        attributeNodes = {
                @NamedAttributeNode("user"),
                @NamedAttributeNode("chat"),
                @NamedAttributeNode("reacts"),
                @NamedAttributeNode("replyMessage")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "pollMessageOptions",
                        type = PollMessage.class,
                        attributeNodes = {
                                @NamedAttributeNode("options")
                        }
                )
        }
)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_id", referencedColumnName = "chat_id")
    private Chat chat;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User user;

    @ManyToOne
    @JoinColumn(name = "reply_message_id", referencedColumnName = "message_id")
    private Message replyMessage;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<React> reacts = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private MessageType messageType;

    private String content;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    private boolean isForwarded = false;

    @Builder.Default
    private boolean isEdited = false;

    @Builder.Default
    @OneToMany(mappedBy = "message", orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<MessageRead> messageReads = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "message", orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<StarredMessage> starredMessages = new HashSet<>();

    @Builder.Default
    private Boolean pinned = false;

    public boolean isStarred(String email) {
        return starredMessages.stream().anyMatch(starredMessage -> starredMessage.getUser().getEmail().equals(email));
    }

    public boolean isSeen(String email) {
        return messageReads.stream().anyMatch(messageRead ->
            messageRead.getUser() != null && messageRead.getUser().getEmail().equals(email) || messageRead.getShowRead()
        );
    }

    public void addReact(React react) {
        reacts.add(react);
    }

}

package com.chatter.chatter.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "invites")
@EntityListeners({ AuditingEntityListener.class })
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invite_id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_chat_id", referencedColumnName = "chat_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private GroupChat groupChat;

    @Builder.Default
    private Boolean canUseLink = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Future
    @Column(updatable = false)
    private Instant expiresAt;

    public boolean isValid() {
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }

}

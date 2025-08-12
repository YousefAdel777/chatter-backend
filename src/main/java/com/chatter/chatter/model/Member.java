package com.chatter.chatter.model;

import com.chatter.chatter.listener.MemberEntityListener;
import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners({AuditingEntityListener.class, MemberEntityListener.class})
@Table(name = "members", uniqueConstraints = { @UniqueConstraint(columnNames = { "user_id", "chat_id" }) })
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(optional = false, cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    @JoinColumn(name = "chat_id", referencedColumnName = "chat_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Chat chat;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MemberRole memberRole = MemberRole.MEMBER;

    @JsonIgnore
    public boolean isAdmin() {
        return memberRole.equals(MemberRole.ADMIN) || memberRole.equals(MemberRole.OWNER);
    }

    @JsonIgnore
    public boolean isOwner() {
        return memberRole.equals(MemberRole.OWNER);
    }

}

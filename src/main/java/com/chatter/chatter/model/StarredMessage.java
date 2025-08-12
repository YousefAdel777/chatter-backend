package com.chatter.chatter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Builder
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@Table(name = "starred_messages", uniqueConstraints = { @UniqueConstraint(columnNames = { "user_id", "message_id" }) })
public class StarredMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "starred_message_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "message_id", referencedColumnName = "message_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnoreProperties("message")
    private Message message;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

}

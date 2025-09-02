package com.chatter.chatter.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "reacts", uniqueConstraints = { @UniqueConstraint(columnNames = { "message_id", "user_id" } ) } )
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class React {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String emoji;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "message_id", referencedColumnName = "message_id")
    private Message message;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

}

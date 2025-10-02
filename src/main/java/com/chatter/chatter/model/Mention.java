package com.chatter.chatter.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
    name = "mentions",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "user_id", "message_id" })},
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_message_id", columnList = "message_id")
    }
)
public class Mention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mention_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "message_id", nullable = false, updatable = false, referencedColumnName = "message_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Message message;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id",  nullable = false, updatable = false, referencedColumnName = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

}

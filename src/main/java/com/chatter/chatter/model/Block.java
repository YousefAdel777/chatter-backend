package com.chatter.chatter.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "blocks",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "blocked_by_id", "blocked_user_id" })
    },
    indexes = {
        @Index(name = "idx_blocked_by_id", columnList = "blocked_by_id"),
        @Index(name = "idx_blocked_user_id", columnList = "blocked_user_id")
    }
)
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "blocked_by_id", referencedColumnName = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User blockedBy;

    @ManyToOne
    @JoinColumn(name = "blocked_user_id", referencedColumnName = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User blockedUser;

}

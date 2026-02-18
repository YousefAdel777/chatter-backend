package com.chatter.chatter.model;

import jakarta.persistence.*;
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
@EntityListeners(AuditingEntityListener.class)
@Table(name = "favorite_gifs", uniqueConstraints = { @UniqueConstraint(name = "uc_user_gif", columnNames = { "user_id", "gif_id" }) })
public class FavoriteGif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_gif_id")
    private Long id;

    @Column(name = "gif_id",  nullable = false, updatable = false)
    private String gifId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

}

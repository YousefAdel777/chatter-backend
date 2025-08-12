package com.chatter.chatter.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name="story_views", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "story_id" }))
public class StoryView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "story_view_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "story_id", referencedColumnName = "story_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Story story;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

}

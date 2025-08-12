package com.chatter.chatter.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EntityListeners(AuditingEntityListener.class)
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "stories")
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "story_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Builder.Default
    @OneToMany(mappedBy = "story", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<StoryView> storyViews = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "story_excluded_users",
        joinColumns = @JoinColumn(name = "story_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> excludedUsers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private StoryType storyType;

    @Column(updatable = false)
    private String content;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public void addExcludedUser(User user) {
        excludedUsers.add(user);
    }

    public void clearExcludedUsers() {
        excludedUsers.clear();
    }

    public boolean isViewed(String email) {
        return storyViews.stream().anyMatch(view -> view.getUser().getEmail().equals(email));
    }

}

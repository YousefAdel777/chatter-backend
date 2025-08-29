package com.chatter.chatter.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "options")
public class Option {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Long id;

    @ManyToOne(optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "message_id", referencedColumnName = "message_id", nullable = false)
    private PollMessage pollMessage;

    @Column(nullable = false)
    private String title;

    @OneToMany(mappedBy = "option", orphanRemoval = true, cascade = CascadeType.REMOVE)
    @Builder.Default
    private List<Vote> votes = new ArrayList<>();

    public void addVote(Vote vote) {
        votes.add(vote);
    }

}

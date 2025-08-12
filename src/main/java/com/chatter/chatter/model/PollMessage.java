package com.chatter.chatter.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@Table(name = "poll_messages")
public class PollMessage extends Message {

    @NotBlank
    @Column(updatable = false, nullable = false)
    private String title;

    @OneToMany(mappedBy = "pollMessage", orphanRemoval = true, cascade = CascadeType.ALL)
    @Builder.Default
    private List<Option> options = new ArrayList<>();

    @Column(updatable = false)
    @Future
    private Instant endsAt;

    @Builder.Default
    private Boolean multiple = true;

    public boolean ended() {
        if (endsAt == null) {
            return false;
        }
        return endsAt.isBefore(Instant.now());
    }

}

package com.chatter.chatter.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@Table(name = "text_stories")
public class TextStory extends Story {

    @Column(nullable = false)
    private String backgroundColor;

    @Column(nullable = false)
    private String textColor;

}

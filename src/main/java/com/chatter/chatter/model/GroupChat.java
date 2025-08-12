package com.chatter.chatter.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@Table(name = "group_chats")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class GroupChat extends Chat {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private String image = "src/static/images/group_image.webp";

    @Builder.Default
    private Boolean onlyAdminsCanSend = false;

    @Builder.Default
    private Boolean onlyAdminsCanInvite = true;

    @Builder.Default
    private Boolean onlyAdminsCanEditGroup = true;

    @Builder.Default
    private Boolean onlyAdminsCanPin = true;

}

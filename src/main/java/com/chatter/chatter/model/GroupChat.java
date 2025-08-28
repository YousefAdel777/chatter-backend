package com.chatter.chatter.model;

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
public class GroupChat extends Chat {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String image;

    @Builder.Default
    private Boolean onlyAdminsCanSend = false;

    @Builder.Default
    private Boolean onlyAdminsCanInvite = true;

    @Builder.Default
    private Boolean onlyAdminsCanEditGroup = true;

    @Builder.Default
    private Boolean onlyAdminsCanPin = true;

}

package com.chatter.chatter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@Table(name = "invite_messages")
public class InviteMessage extends Message {

    @ManyToOne
    @JoinColumn(name = "invite_id", referencedColumnName = "invite_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Invite invite;

}

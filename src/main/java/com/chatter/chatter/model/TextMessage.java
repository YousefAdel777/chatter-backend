package com.chatter.chatter.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder(toBuilder = true)
@Table(name = "text_messages")
@AllArgsConstructor
public class TextMessage extends Message {

}

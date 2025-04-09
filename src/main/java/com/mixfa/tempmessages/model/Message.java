package com.mixfa.tempmessages.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
//@JsonSubTypes({
//        @JsonSubTypes.Type(value = TextMessage.class, name = "TEXT"),
//        @JsonSubTypes.Type(value = FileMessage.class, name = "FILE")
//})
public sealed interface Message extends Serializable permits FileMessage, TextMessage {
    Type type();

    Dto toDto();

    enum Type {
        TEXT,
        FILE
    }

    interface Dto {
        Type type();
    }
}

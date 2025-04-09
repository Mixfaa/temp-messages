package com.mixfa.tempmessages.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TextMessage(
        String text
) implements Message, Message.Dto {
    @Override
    @JsonProperty
    public Type type() {
        return Type.TEXT;
    }

    @Override
    public Dto toDto() {
        return this;
    }
}

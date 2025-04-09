package com.mixfa.tempmessages.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FileMessage(
        @JsonIgnore
        String path,
        String id
) implements Message, Message.Dto {
    @Override
    @JsonProperty
    public Type type() {
        return Type.FILE;
    }

    @Override
    public Message.Dto toDto() {
        return this;
    }

    public FileMessage(FileData fileData) {
        this(fileData.path(), fileData.id());
    }
}

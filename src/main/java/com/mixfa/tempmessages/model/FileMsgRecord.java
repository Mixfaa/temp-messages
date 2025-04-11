package com.mixfa.tempmessages.model;

public record FileMsgRecord(
        String channelName,
        String messageId) {
    public String record() {
        return channelName + ":" + messageId;
    }
}

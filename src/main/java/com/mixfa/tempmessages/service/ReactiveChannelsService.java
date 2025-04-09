package com.mixfa.tempmessages.service;

import com.mixfa.tempmessages.model.Channel;
import com.mixfa.tempmessages.model.FileResponse;
import com.mixfa.tempmessages.model.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;

public interface ReactiveChannelsService {
    Flux<Message> listenMessages(String channelName, String password) throws Exception;

    Flux<Message> listMessages(String channelName, String password, long rangeStart, long rangeEnd) throws Exception;

    Mono<Channel> createChannel(String channelName, String password) throws Exception;

    Mono<Message> sendMessage(String channelName, String password, Message payload) throws Exception;

    Mono<Message> sendFileMessage(String channelName, String password, String filename, InputStream inputStream) throws Exception;

    Mono<FileResponse> getFile(String channelName, String password, String id) throws Exception;

    boolean channelExists(String channelName);

    boolean channelCheckCredentials(String channelName, String password);
}

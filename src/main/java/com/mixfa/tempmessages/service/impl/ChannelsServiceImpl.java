package com.mixfa.tempmessages.service.impl;

import com.mixfa.tempmessages.model.Channel;
import com.mixfa.tempmessages.model.FileMessage;
import com.mixfa.tempmessages.model.FileResponse;
import com.mixfa.tempmessages.model.Message;
import com.mixfa.tempmessages.service.ChannelStorage;
import com.mixfa.tempmessages.service.FileStorageService;
import com.mixfa.tempmessages.service.ReactiveChannelsService;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

@Getter
@Accessors(fluent = true)
class ChannelWithMessages extends Channel {
    private final List<Message> messages = new CopyOnWriteArrayList<>();
    private final Sinks.Many<Message> messageSink;
    private final Flux<Message> messageFlux;

    private final Set<String> filesIDs = new CopyOnWriteArraySet<>();

    public ChannelWithMessages(String name, String passwordHash) {
        super(name, passwordHash);
        this.messageSink = Sinks.many().multicast().onBackpressureBuffer();
        this.messageFlux = messageSink.asFlux().share();
    }
}

@Slf4j
@Service
@Profile("standalone")
public class ChannelsServiceImpl implements ReactiveChannelsService {
    private final ChannelStorage<ChannelWithMessages> channelStorage;
    private final FileStorageService fileStorageService;

    public ChannelsServiceImpl(FileStorageService fileStorageService) {
        this.channelStorage = ChannelStorage.local(ChannelWithMessages::new);
        this.fileStorageService = fileStorageService;

        channelStorage.subscribeToChannelDestruction(this::onChannelDestruction);
    }

    private void onChannelDestruction(ChannelWithMessages channel) {
        channel.messageSink().tryEmitComplete();
        for (String filesID : channel.filesIDs()) {
            try {
                fileStorageService.delete(filesID);
            } catch (Exception ex) {
                log.error(ex.getLocalizedMessage());
            }
        }
    }

    @Override
    public Flux<Message> listenMessages(String channelName, String password) throws Exception {
        var channel = channelStorage.getChannelOrCreate(channelName, password);
        return channel.messageFlux();
    }

    @Override
    public Flux<Message> listMessages(String channelName, String password, long offset, long limit) throws Exception {
        return Flux.fromStream(channelStorage.getChannelOrCreate(channelName, password)
                .messages().stream().skip(offset).limit(limit));
    }

    @Override
    public Mono<Channel> createChannel(String channelName, String password) throws Exception {
        return Mono.fromSupplier(() -> {
            try {
                return channelStorage.createChannel(channelName, password);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Mono<Message> sendMessage(String channelName, String password, Message payload) throws Exception {
        var channel = channelStorage.getChannelOrThrow(channelName, password);
        return Mono.fromSupplier(() -> {
            if (payload instanceof FileMessage fileMessage)
                channel.filesIDs().add(fileMessage.id());

            channel.messageSink().tryEmitNext(payload);
            channel.messages().add(payload);

            return payload;
        });
    }

    @Override
    public Mono<Message> sendFileMessage(String channelName, String password, String filename, InputStream inputStream) throws Exception {
        var savedFile = fileStorageService.write(filename, inputStream);
        return sendMessage(channelName, password, new FileMessage(savedFile));
    }

    @Override
    public Mono<FileResponse> getFile(String channelName, String password, String id) throws Exception {
        var channel = channelStorage.getChannelOrThrow(channelName, password);
        if (!channel.filesIDs().contains(id))
            throw new Exception("File not found");

        return Mono.fromSupplier(()  -> {
            try {
                return fileStorageService.read(id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelStorage.channelExists(channelName);
    }

    @Override
    public boolean channelCheckCredentials(String channelName, String password) {
        return channelStorage.channelCheckCredentials(channelName, password);
    }
}

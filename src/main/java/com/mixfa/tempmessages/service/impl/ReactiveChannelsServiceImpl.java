package com.mixfa.tempmessages.service.impl;

import com.mixfa.tempmessages.model.Channel;
import com.mixfa.tempmessages.model.FileMessage;
import com.mixfa.tempmessages.model.FileResponse;
import com.mixfa.tempmessages.model.Message;
import com.mixfa.tempmessages.service.FileStorageService;
import com.mixfa.tempmessages.service.ReactiveChannelsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveListOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

record FileMsgRecord(
        String channelName,
        String messageId) {
    public String record() {
        return channelName + ":" + messageId;
    }
}

@Service
@Slf4j
public class ReactiveChannelsServiceImpl implements ReactiveChannelsService {
    private final ReactiveRedisMessageListenerContainer messageListenerContainer;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    private final Map<String, Channel> channelsStorage = new ConcurrentHashMap<>();
    private final ReactiveListOperations<String, Object> listOps;
    private final ReactiveValueOperations<String, Object> valueOps;
    private final LocalFileStorageService localFileStorageService;
    private final SerializationPair<Object> serializationPair;

    private final static SerializationPair<String> STRING_SERIALIZATION_PAIR = SerializationPair
            .fromSerializer(RedisSerializer.string());

    private static final ScheduledExecutorService scheduledExecutor = Executors
            .newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

    public ReactiveChannelsServiceImpl(ReactiveRedisMessageListenerContainer messageListenerContainer,
            ReactiveRedisTemplate<String, Object> redisTemplate, PasswordEncoder passwordEncoder,
            FileStorageService fileStorageService, RedisSerializer<Object> redisSerializer,
            LocalFileStorageService localFileStorageService) {
        this.messageListenerContainer = messageListenerContainer;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.listOps = redisTemplate.opsForList();
        this.valueOps = redisTemplate.opsForValue();
        this.fileStorageService = fileStorageService;
        this.localFileStorageService = localFileStorageService;

        this.serializationPair = SerializationPair.fromSerializer(redisSerializer);
    }

    @Override
    public Flux<Message> listenMessages(String channelName, String password) throws Exception {
        var channel = getChannelOrCreate(channelName, password);

        var flux = messageListenerContainer
                .receive(List.of(channel.topic()), STRING_SERIALIZATION_PAIR, serializationPair)
                .map(ReactiveSubscription.Message::getMessage)
                .cast(Message.class);

        channel.addResource(flux);

        return flux;
    }

    @Override
    public Flux<Message> listMessages(String channelName, String password, long start, long end) throws Exception {
        getChannelOrCreate(channelName, password);

        return listOps.range(channelName, start, end)
                .cast(Message.class);
    }

    @Override
    public Mono<Channel> createChannel(String channelName, String password) throws Exception {
        if (channelsStorage.containsKey(channelName))
            throw new Exception("Channel with this name already exists");

        return Mono.just(createChannelAnyway(channelName, password));
    }

    @Override
    public Mono<Message> sendMessage(String channelName, String password, Message payload) throws Exception {
        var channel = getChannelOrCreate(channelName, password);

        return listOps.leftPush(channelName, payload)
                .then(redisTemplate.convertAndSend(channel.topic().getTopic(), payload))
                .then(Mono.defer(() -> {
                    if (payload instanceof FileMessage fileMessage) {
                        var fileMsgRecord = new FileMsgRecord(channelName, fileMessage.id());

                        channel.addResource(new WeakReference<>(fileMsgRecord));
                        return valueOps.set(fileMsgRecord.record(), passwordEncoder.encode(password));
                    }
                    return Mono.empty();
                }))
                .thenReturn(payload);
    }

    @Override
    public Mono<Message> sendFileMessage(String channelName, String password, String filename, InputStream inputStream)
            throws Exception {
        var savedFile = localFileStorageService.write(filename, inputStream);
        return sendMessage(channelName, password, new FileMessage(savedFile));
    }

    @Override
    public Mono<FileResponse> getFile(String channelName, String password, String id) {
        var fileMsgRecord = new FileMsgRecord(channelName, id);

        return valueOps.get(fileMsgRecord.record())
                .switchIfEmpty(Mono.error(new Exception("File not found")))
                .map(passwordHash -> {
                    if (passwordHash instanceof String hash) {
                        if (!passwordEncoder.matches(password, hash))
                            throw new RuntimeException("Password not match");
                        try {
                            return fileStorageService.read(id);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    throw new RuntimeException("Password hash is not instance of String");
                });
    }

    @Override
    public boolean channelCheckCredentials(String channelName, String password) {
        var channel = channelsStorage.get(channelName);
        if (channel == null)
            return false;

        return passwordEncoder.matches(password, channel.passwordHash());
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelsStorage.containsKey(channelName);
    }

    private void deleteChannelAfterTimeout(Channel channel) {
        channelsStorage.remove(channel.name());
        listOps.delete(channel.name()).subscribe();

        for (Object resource : channel.filteredResources()) {
            switch (resource) {
                case Disposable disposable -> disposable.dispose();
                case FileMsgRecord fileMsgRecord -> {
                    valueOps.delete(fileMsgRecord.record()).subscribe();
                    try {
                        localFileStorageService.delete(fileMsgRecord.messageId());
                    } catch (Exception e) {
                        log.error(e.getLocalizedMessage());
                    }
                }
                default -> log.error("Can`t handle resource ({}) {}", resource.getClass(), resource);
            }
        }
    }

    private Channel createChannelAnyway(String name, String password) {
        var channel = new Channel(name, passwordEncoder.encode(password));
        channelsStorage.put(name, channel);

        scheduledExecutor.schedule(() -> {
            try {
                deleteChannelAfterTimeout(channel);
            } catch (Exception ex) {
                log.error(ex.getLocalizedMessage());
            }
        }, 1, TimeUnit.DAYS);

        return channel;
    }

    private Channel getChannelOrCreate(String name, String password) throws Exception {
        var channel = channelsStorage.get(name);
        if (channel == null) {
            return createChannelAnyway(name, password);
        }
        if (!passwordEncoder.matches(password, channel.passwordHash()))
            throw new Exception("Password not matches");

        return channel;
    }

    private Channel getChannelOrThrow(String name, String password) throws Exception {
        var channel = channelsStorage.get(name);
        if (channel == null)
            throw new Exception("Channel not found");
        if (!passwordEncoder.matches(password, channel.passwordHash()))
            throw new Exception("Password not matches");

        return channel;
    }
}

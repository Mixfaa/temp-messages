package com.mixfa.tempmessages.service.impl;

import com.mixfa.tempmessages.model.Channel;
import com.mixfa.tempmessages.service.ChannelStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class MapChannelStorageImpl<ChannelType extends Channel> implements ChannelStorage<ChannelType> {
    private final Map<String, ChannelType> channelsStorage = new ConcurrentHashMap<>();
    private final BiFunction<String, String, ChannelType> creator;
    private final PasswordEncoder passwordEncoder;
    private final Duration timeToLive;

    private final List<Consumer<ChannelType>> destroyHandlers = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

    private ChannelType createChannelAnyway(String name, String password) {
        var channel = creator.apply(name, passwordEncoder.encode(password));
        channelsStorage.put(name, channel);

        scheduledExecutor.schedule(() -> {
            for (Consumer<ChannelType> destroyHandler : destroyHandlers) {
                try {
                    destroyHandler.accept(channel);
                } catch (Exception ex) {
                    log.error(ex.getLocalizedMessage());
                    ex.printStackTrace();
                }

                channelsStorage.remove(name);
            }
        }, timeToLive.toSeconds(), TimeUnit.SECONDS);

        return channel;
    }

    @Override
    public void deleteChannel(String name) {
        channelsStorage.remove(name);
    }

    @Override
    public ChannelType createChannel(String name, String password) throws Exception {
        if (channelsStorage.containsKey(name))
            throw new Exception("Channel with this name already exists");

        return createChannelAnyway(name, password);
    }

    @Override
    public ChannelType getChannelOrCreate(String name, String password) throws Exception {
        var channel = channelsStorage.get(name);
        if (channel == null) {
            return createChannelAnyway(name, password);
        }
        if (!passwordEncoder.matches(password, channel.passwordHash()))
            throw new Exception("Password not matches");

        return channel;
    }

    @Override
    public ChannelType getChannelOrThrow(String name, String password) throws Exception {
        var channel = channelsStorage.get(name);
        if (channel == null)
            throw new Exception("Channel not found");
        if (!passwordEncoder.matches(password, channel.passwordHash()))
            throw new Exception("Password not matches");

        return channel;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelsStorage.containsKey(channelName);
    }

    @Override
    public boolean channelCheckCredentials(String channelName, String password) {
        var channel = channelsStorage.get(channelName);
        if (channel == null)
            return false;

        return passwordEncoder.matches(password, channel.passwordHash());
    }

    @Override
    public void subscribeToChannelDestruction(Consumer<ChannelType> handler) {
        this.destroyHandlers.add(handler);
    }
}

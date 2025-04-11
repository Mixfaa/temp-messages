package com.mixfa.tempmessages.service;

import com.mixfa.tempmessages.TempMessagesApplication;
import com.mixfa.tempmessages.model.Channel;
import com.mixfa.tempmessages.service.impl.MapChannelStorageImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface ChannelStorage<ChannelType extends Channel> {
    void deleteChannel(String name);

    ChannelType createChannel(String name, String password) throws Exception;

    ChannelType getChannelOrCreate(String name, String password) throws Exception;

    ChannelType getChannelOrThrow(String name, String password) throws Exception;

    boolean channelExists(String channelName);

    boolean channelCheckCredentials(String channelName, String password);

    void subscribeToChannelDestruction(Consumer<ChannelType> handler);

    static <T extends Channel> ChannelStorage<T> local(BiFunction<String, String, T> constructor) {
        var passwordEncoder = TempMessagesApplication.getApplicationContext().getBean(PasswordEncoder.class);
        return new MapChannelStorageImpl<>(constructor, passwordEncoder, Duration.ofDays(1));
    }
}

package com.mixfa.tempmessages.model;


import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.context.ApplicationEvent;

public sealed interface ChannelEvent{
    Channel channel();

    @Accessors(fluent = true)
    final class Created extends ApplicationEvent implements ChannelEvent {
        @Getter
        private final String name;
        @Getter
        private final String password;
        private final Channel channel;

        public Created(String name, String password, Channel channel, Object source) {
            super(source);
            this.name = name;
            this.password = password;
            this.channel = channel;
        }

        @Override
        public Channel channel() {
            return channel;
        }
    }

    final class Deleted extends ApplicationEvent implements ChannelEvent {
        private final Channel channel;

        public Deleted(Channel channel, Object source) {
            super(source);
            this.channel = channel;
        }

        @Override
        public Channel channel() {
            return channel;
        }
    }
}



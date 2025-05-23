package com.mixfa.tempmessages.model;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.redis.listener.ChannelTopic;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

 
@Getter
@ToString
@Accessors(fluent = true)
public class Channel {
    private final String name;
    private final String passwordHash;
    private final ChannelTopic topic;
    private final Instant creationTime;
    private final List<Object> resources;

    public Channel(
            String name,
            String passwordHash) {
        this.name = name;
        this.passwordHash = passwordHash;
        this.topic = new ChannelTopic(name);
        this.creationTime = Instant.now();
        this.resources = new CopyOnWriteArrayList<>();
    }

    public <T> T addResource(T resource) {
        this.resources.add(resource);
        return resource;
    }

    public Iterable<Object> filteredResources() {
        return resources.stream()
                .map(res -> switch (res) {
                    case WeakReference<?> weakRef -> weakRef.get();
                    case SoftReference<?> softRef -> softRef.get();
                    default -> res;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public <T> List<T> findResourcesOfType(Class<T> clazz) {
        return resources
                .stream()
                .filter(clazz::isInstance)
                .map(res -> (T) res)
                .toList();
    }


    public <T> Optional<T> findResourceOfType(Class<T> clazz) {
        return resources
                .stream()
                .filter(clazz::isInstance)
                .findFirst()
                .map(res -> (T) res);
    }

    public void clearResources() {
        this.resources.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (Channel) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.passwordHash, that.passwordHash) &&
                Objects.equals(this.topic, that.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, passwordHash, topic);
    }
}

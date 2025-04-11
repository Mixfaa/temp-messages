package com.mixfa.tempmessages.config;

import com.mixfa.tempmessages.TempMessagesApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Path;

@Profile("redis")
@Slf4j
@Configuration
@SpringBootApplication
public class RedisConfig {
    @Bean
    public ReactiveRedisMessageListenerContainer reactiveRedisMessageListenerContainer(ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisMessageListenerContainer(factory);
    }

    @Bean
    public RedisSerializer<Object> valueRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer(TempMessagesApplication.getObjectMapper());
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory, RedisSerializer<Object> serializer) {
        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, Object> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public CommandLineRunner flushAll(ReactiveRedisTemplate<String, Object> template, @Value("${filestorage.root}") String root) {
        return _ -> {
            try {
                template.getConnectionFactory().getReactiveConnection().serverCommands().flushAll().subscribe();
                FileSystemUtils.deleteRecursively(Path.of(root));
            } catch (Exception ex) {
                log.error(ex.getLocalizedMessage());
            }
        };
    }
}

package com.mixfa.tempmessages.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("standalone")
@Configuration
@SpringBootApplication(exclude = {
        RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class
})
public class StandaloneConfig {
}

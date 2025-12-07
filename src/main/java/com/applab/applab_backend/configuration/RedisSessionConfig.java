package com.applab.applab_backend.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

import java.time.Duration;

@Configuration
@EnableRedisIndexedHttpSession
public class RedisSessionConfig {

    @Value("${spring.session.redis.namespace}")
    private String namespace;

    // Read session timeout from application.properties as a Duration
    // Supports human-readable formats like "1d", "24h", "3600s"
    @Value("${spring.session.timeout}")
    private Duration sessionTimeout;

    @Bean
    public static ConfigureRedisAction configureRedisAction() {
        return ConfigureRedisAction.NO_OP;
    }

    @Bean
    @Primary
    public RedisIndexedSessionRepository customizeSessionRepository(RedisIndexedSessionRepository repository) {
        repository.setRedisKeyNamespace(namespace);
        repository.setDefaultMaxInactiveInterval(sessionTimeout); // Set session timeout (TTL) in Redis using Duration
        return repository;
    }
}
package com.applab.applab_backend.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.context.annotation.Bean;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

@Configuration
@EnableRedisIndexedHttpSession  // Enables Spring Session stored inside Redis using indexed repository
public class RedisSessionConfig {

    // Load the namespace FROM application.properties
    // Example:
    //   spring.session.redis.namespace=applab-local-sessions
    @Value("${spring.session.redis.namespace}")
    private String namespace;

    /**
     * Many VPS Redis servers disable CONFIG command for security.
     * Spring Session tries "CONFIG SET notify-keyspace-events EEx".
     * This fails and breaks session creation.
     *
     * Returning NO_OP means:
     * "Do not try to run CONFIG SET. Just continue normally."
     */
    @Bean
    public static ConfigureRedisAction configureRedisAction() {
        return ConfigureRedisAction.NO_OP;
    }

    /**
     * Spring Boot automatically creates RedisIndexedSessionRepository.
     * But it applies its own default namespace:
     *      spring:session
     *
     * We override the namespace here.
     *
     * Important:
     *  - This does NOT break default Boot configuration.
     *  - This runs AFTER the repository is created.
     *  - This is safe for both LOCAL & PRODUCTION.
     */
    @Bean
    @Primary
    public RedisIndexedSessionRepository customizeSessionRepository(RedisIndexedSessionRepository repository) {

        // Set the namespace. Example:
        // redis keys will become:
        //   applab-local-sessions:sessions:<sessionId>
        repository.setRedisKeyNamespace(namespace);

        return repository;
    }
}
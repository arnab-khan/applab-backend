package com.applab.applab_backend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.web.http.HttpSessionIdResolver;

import com.applab.applab_backend.common.component.HeaderSessionIdResolver;


@Configuration
public class AppConfig {

    // PasswordEncoder bean.
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCrypt hashing algorithm
    }

    // HttpSessionIdResolver bean to use custom header for session ID.
    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        return new HeaderSessionIdResolver();
    }
}

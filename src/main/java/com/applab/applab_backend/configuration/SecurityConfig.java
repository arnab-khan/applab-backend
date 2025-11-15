package com.applab.applab_backend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // disable CSRF for APIs, because it breaks APIs It expects a CSRF token APIs don’t send CSRF tokens (Angular, React, Vue use JWT tokens ≠ CSRF tokens) So will get 403 Forbidden without disabling it
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/signup", "/auth/login").permitAll()  // allow public
                .anyRequest().authenticated()  // other endpoints need login
            );

        return http.build();
    }
}
package com.applab.applab_backend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.applab.applab_backend.common.component.HeaderSessionAuthFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    private final HeaderSessionAuthFilter headerSessionAuthFilter;

    public SecurityConfig(HeaderSessionAuthFilter headerSessionAuthFilter) {
        this.headerSessionAuthFilter = headerSessionAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF since this is probably a stateless API using tokens or headers
                .csrf(csrf -> csrf.disable())

                // Configure authorization rules for HTTP requests
                .authorizeHttpRequests(auth -> auth
                        // Allow public endpoints and authentication endpoints without any authentication
                        .requestMatchers("/public/**", "/auth/**").permitAll()

                        // Only allow users with ROLE_ADMIN to access any URL containing '/admin/'
                        .requestMatchers(request -> request.getRequestURI().contains("/admin/")).hasRole("ADMIN")

                        // Allow users with ROLE_USER or ROLE_ADMIN to access URLs containing '/user/' This ensures admins can access user endpoints as well
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                    )

                // Configure custom handlers for 401 and 403
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(customAuthenticationEntryPoint())
                    .accessDeniedHandler(customAccessDeniedHandler())
                )

                // Disable default HTTP Basic authentication
                .httpBasic(basic -> basic.disable())

                // Disable default form login
                .formLogin(form -> form.disable());

        // Add custom filter before the default Spring Security UsernamePasswordAuthenticationFilter
        // This filter restores SecurityContext from the session ID in the custom header (X-SESSION-ID)
        http.addFilterBefore(headerSessionAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // Build and return the configured filter chain
        return http.build();
    }

    // Handles cases where there is NO valid authentication (user is not logged in or session is missing/expired/invalid)
    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized: Please login\"}");
        };
    }

    // Handles cases where the user is authenticated but does NOT have permission for the requested resource
    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Forbidden: Access denied\"}");
        };
    }
}
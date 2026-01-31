package com.applab.applab_backend.common.component;

import java.io.IOException;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class HeaderSessionAuthFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        System.out.println(path.contains("/public/"));
        return path.startsWith("/auth/") || path.contains("/public/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Retrieve the existing session; do NOT create a new one
        HttpSession session = request.getSession(false);

        if (session == null) {
            // No session exists -> invalid or expired session
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or expired session\"}");
            return;
        }

        // Retrieve the SecurityContext saved during login from the session
        Object savedContext = session.getAttribute("securityContext");

        // Save the current SecurityContext to restore later
        SecurityContext previousContext = SecurityContextHolder.getContext();
        boolean contextRestored = false;

        if (savedContext instanceof SecurityContext securityContext) {
            SecurityContextHolder.setContext(securityContext);
            contextRestored = true;
        }

        try {
            // Continue filter chain with restored SecurityContext
            filterChain.doFilter(request, response);
        } finally {
            // Restore previous SecurityContext to avoid affecting other requests
            if (contextRestored) {
                SecurityContextHolder.clearContext();
                SecurityContextHolder.setContext(previousContext);
            }
        }
    }
}

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

/**
 * Filter to restore Spring Security context from a session stored in Redis.
 * 
 * This filter intercepts every request (except public/auth endpoints) and ensures
 * the SecurityContext is loaded into the current thread. It retrieves the session
 * from the session store (Redis in this case) using the session ID from the request header.
 */
@Component
public class HeaderSessionAuthFilter extends OncePerRequestFilter {

    /**
     * Determines whether this filter should be skipped for a given request.
     * 
     * We skip public and authentication endpoints because they do not require a session.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/") || path.startsWith("/public/");
    }

    /**
     * Restores the SecurityContext from the session if present.
     * If no valid session exists, the request is rejected with 401 Unauthorized.
     */
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

        // Save the current SecurityContext to restore later (to prevent leaking between requests)
        SecurityContext previousContext = SecurityContextHolder.getContext();
        boolean contextRestored = false;

        if (savedContext instanceof SecurityContext securityContext) {
            // Restore the authenticated user's SecurityContext for this request.
            // Spring Security uses SecurityContextHolder to check roles/permissions.
            // If we don't set this, the user will not be recognized as logged-in (403 errors on protected APIs).
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

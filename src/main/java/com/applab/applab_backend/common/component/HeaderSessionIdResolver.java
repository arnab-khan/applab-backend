package com.applab.applab_backend.common.component;

import java.util.Collections;
import java.util.List;

import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// This class tells Spring Session how to read/write session IDs from/to HTTP requests and responses
// Specifically, it uses a custom header called "X-SESSION-ID" instead of cookies
@Component
public class HeaderSessionIdResolver implements HttpSessionIdResolver {

    // Name of the HTTP header that will carry the session ID
    private static final String HEADER_NAME = "X-SESSION-ID";

    // This method's only job is to read session IDs from the incoming request.
    // It does not store anything itself. It simply returns a List<String> of session IDs.
    // By default, Spring Session expects session IDs in cookies (usually named SESSION).
    // This method overrides that default behavior to read the session ID from a custom header instead.
    // Spring Session then uses these IDs to fetch the session from the session store (Redis) and associate it with the current request.
    // Therefore, calling request.getSession(false) will return the session if it exists.
    @Override
    public List<String> resolveSessionIds(HttpServletRequest request) {

        // Read the session ID from the API request header
        String sessionId = request.getHeader(HEADER_NAME);

        // If the header is not present, return an empty list
        if (sessionId == null) {
            return Collections.emptyList();
        }

        // If sessionId exists, return it as a list.
        // Spring Session supports multiple session IDs per request, hence we return a List format
        return List.of(sessionId);
    }

    // This method is called by Spring Session when it wants to send the session ID back to the client.
    // By default, Spring Session stores session IDs in cookies (usually named SESSION), 
    // which the client sends with every request automatically.
    // Here, we override that default behavior to use a custom header instead of cookies.
    // The client is expected to read the "X-SESSION-ID" header and send it in subsequent requests.
    @Override
    public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {

        // Set the session ID in the response header instead of a cookie.
        response.setHeader(HEADER_NAME, sessionId);
    }


    // Called by Spring Session when a session needs to be expired (e.g., on logout)
    // By default, Spring Session would remove the session cookie from the client.
    // Since we are using a custom header instead of cookies, this method overrides the default behavior.
    // It removes the session ID from the response header, effectively telling the client that the session is expired.
    @Override
    public void expireSession(HttpServletRequest request, HttpServletResponse response) {

        // Removes the session ID from the response header
        response.setHeader(HEADER_NAME, "");
    }

}
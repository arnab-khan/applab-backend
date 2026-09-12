package com.applab.applab_backend.configuration;

import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.applab.applab_backend.common.constant.WebSocketDestination;

import jakarta.servlet.http.Cookie;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final ChatRoomWebSocketInterceptor chatRoomWebSocketInterceptor;

    public WebSocketConfig(@Lazy ChatRoomWebSocketInterceptor chatRoomWebSocketInterceptor) {
        this.chatRoomWebSocketInterceptor = chatRoomWebSocketInterceptor;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatRoomWebSocketInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        // Client will subscribe to /topic/...
        config.enableSimpleBroker(WebSocketDestination.TOPIC);

        // Client will send messages to /app/...
        config.setApplicationDestinationPrefixes(WebSocketDestination.APP);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        // Frontend connects here → http://localhost:8080/ws
        registry.addEndpoint("/ws")
                .addInterceptors(new HttpSessionHandshakeInterceptor(), guestIdHandshakeInterceptor())
                .setAllowedOriginPatterns("http://localhost:*", "http://192.168.0.*:*", "https://applab.arnabkhan.in");
    }

    // Copies the cookie into the WebSocket session so STOMP handlers can
    // identify guests without requiring the frontend to send in messages.
    private HandshakeInterceptor guestIdHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    Cookie[] cookies = servletRequest.getServletRequest().getCookies();
                    if (cookies != null) {
                        for (Cookie cookie : cookies) {
                            if ("guestId".equals(cookie.getName())) {
                                attributes.put("guestId", cookie.getValue());
                                break;
                            }
                        }
                    }
                }

                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                    WebSocketHandler wsHandler, Exception exception) {
            }
        };
    }
}

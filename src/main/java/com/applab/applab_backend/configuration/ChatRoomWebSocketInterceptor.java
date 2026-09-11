package com.applab.applab_backend.configuration;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.applab.applab_backend.chatroom.service.ChatRoomService;
import com.applab.applab_backend.message.enums.MessageOperation;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatRoomWebSocketInterceptor implements ChannelInterceptor {
    private static final Pattern ROOM_TOPIC = Pattern.compile("/topic/chatroom/([1-9][0-9]*)/(message|typing|read)");
    private static final Pattern USER_CHAT_TOPIC = Pattern.compile("/topic/user/([1-9][0-9]*)/chatroom-update");
    private final ChatRoomService chatRoomService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        String destination = accessor.getDestination();
        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            if ("/topic/chatroom-message".equals(destination) || "/topic/chatroom-typing".equals(destination)) {
                return message;
            }
            Map<String, Object> attributes = accessor.getSessionAttributes();
            Long userId = attributes != null && attributes.get("userId") instanceof Long id ? id : null;
            Matcher userMatcher = USER_CHAT_TOPIC.matcher(destination == null ? "" : destination);
            if (userMatcher.matches()) {
                if (userId == null || !userId.equals(Long.valueOf(userMatcher.group(1)))) {
                    throw new AccessDeniedException("Subscription not allowed");
                }
                return message;
            }
            Matcher matcher = ROOM_TOPIC.matcher(destination == null ? "" : destination);
            if (!matcher.matches()) {
                throw new AccessDeniedException("Subscription not allowed");
            }
            chatRoomService.requireChatRoomPermission(MessageOperation.GET, Long.valueOf(matcher.group(1)), userId);
        } else if (accessor.getCommand() == StompCommand.SEND
                && !"/app/chatroom-typing".equals(destination)) {
            // Only application handlers may publish broker events.
            throw new AccessDeniedException("Destination not allowed");
        }
        return message;
    }
}

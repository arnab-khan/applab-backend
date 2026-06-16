package com.applab.applab_backend.chatroom.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.applab.applab_backend.chatroom.dto.ChatRoomMessageResponse;
import com.applab.applab_backend.chatroom.dto.ChatRoomReactionPageResponse;
import com.applab.applab_backend.chatroom.dto.ChatRoomRequest;
import com.applab.applab_backend.chatroom.dto.CursorPageResponse;
import com.applab.applab_backend.chatroom.dto.GlobalChatRoomResponse;
import com.applab.applab_backend.chatroom.service.ChatRoomService;
import com.applab.applab_backend.message.enums.MessageDirection;
import com.applab.applab_backend.message.dto.EditMessageRequest;
import com.applab.applab_backend.reaction.dto.ReactionEmojiRequest;
import com.applab.applab_backend.reaction.dto.ReactionCountResponse;
import com.applab.applab_backend.reaction.model.ReactionModel;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chatroom")
@RequiredArgsConstructor
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    @GetMapping("/global")
    public GlobalChatRoomResponse getGlobalChatRoom() {
        return new GlobalChatRoomResponse(chatRoomService.getGlobalChatRoomId());
    }

    @MessageMapping("/chatroom-typing")
    public void publishTyping(Map<String, Object> request, SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        String guestId = sessionAttributes != null ? (String) sessionAttributes.get("guestId") : null;
        Long userId = sessionAttributes != null ? (Long) sessionAttributes.get("userId") : null;

        if (userId == null && guestId == null && request.get("guestId") instanceof String requestGuestId) {
            guestId = requestGuestId;
        }

        chatRoomService.publishChatRoomTyping(((Number) request.get("chatRoomId")).longValue(), guestId, userId);
    }

    @GetMapping("/{chatRoomId}/message/all")
    public CursorPageResponse<ChatRoomMessageResponse> getMessages(
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Long uptoId,
            @RequestParam(defaultValue = "OLDER") MessageDirection direction,
            @RequestParam(defaultValue = "10") int limit,
            @CookieValue(required = false) String guestId,
            HttpSession session) {
        return chatRoomService.getChatRoomMessages(chatRoomId, parentId, deleted, cursor, uptoId, direction, limit, guestId,
                session);
    }

    @PostMapping("/{chatRoomId}/message/add")
    public ChatRoomMessageResponse addMessage(@PathVariable Long chatRoomId, @Valid @RequestBody ChatRoomRequest chatRoom,
            @CookieValue(required = false) String guestId, HttpSession session) {
        return chatRoomService.addChatRoomMessage(chatRoomId, chatRoom, guestId, session);
    }

    @PatchMapping("/{chatRoomId}/message/edit")
    public ChatRoomMessageResponse editMessage(@PathVariable Long chatRoomId, @Valid @RequestBody EditMessageRequest message,
            @CookieValue(required = false) String guestId, HttpSession session) {
        return chatRoomService.editChatRoomMessage(chatRoomId, message, guestId, session);
    }

    @DeleteMapping("/{chatRoomId}/message/{messageId}/delete")
    public void deleteMessage(@PathVariable Long chatRoomId, @PathVariable Long messageId,
            @CookieValue(required = false) String guestId, HttpSession session) {
        chatRoomService.deleteChatRoomMessage(chatRoomId, messageId, guestId, session);
    }

    @PostMapping("/message/{messageId}/reaction/add")
    public ReactionModel addReaction(@PathVariable Long messageId,
            @Valid @RequestBody ReactionEmojiRequest reaction, @CookieValue(required = false) String guestId,
            HttpSession session) {
        return chatRoomService.addChatRoomMessageReaction(messageId, reaction, guestId, session);
    }

    @GetMapping("/{chatRoomId}/message/{messageId}/reaction/all")
    public ChatRoomReactionPageResponse getReactions(
            @PathVariable Long chatRoomId,
            @PathVariable Long messageId,
            @RequestParam(required = false) String emoji,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit,
            @CookieValue(required = false) String guestId,
            HttpSession session) {
        return chatRoomService.getChatRoomMessageReactions(chatRoomId, messageId, emoji, cursor, limit, guestId,
                session);
    }

    @GetMapping("/{chatRoomId}/message/{messageId}/reaction/count/all")
    public List<ReactionCountResponse> getReactionCounts(
            @PathVariable Long chatRoomId,
            @PathVariable Long messageId,
            @CookieValue(required = false) String guestId,
            HttpSession session) {
        return chatRoomService.getChatRoomMessageReactionCounts(chatRoomId, messageId, guestId, session);
    }
}

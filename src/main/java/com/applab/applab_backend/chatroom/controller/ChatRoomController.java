package com.applab.applab_backend.chatroom.controller;

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
import com.applab.applab_backend.chatroom.dto.ChatRoomRequest;
import com.applab.applab_backend.chatroom.dto.CursorPageResponse;
import com.applab.applab_backend.chatroom.service.ChatRoomService;
import com.applab.applab_backend.message.dto.OptionalMessageRequest;
import com.applab.applab_backend.message.model.MessageModel;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/chatroom")
@RequiredArgsConstructor
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    @GetMapping("/global/message/all")
    public CursorPageResponse<ChatRoomMessageResponse> getGlobalMessages(
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "false") Boolean deleted,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit,
            @CookieValue(required = false) String guestId,
            HttpSession session) {
        return chatRoomService.getChatRoomMessages(chatRoomService.getGlobalChatRoomId(), parentId, deleted, cursor,
                limit, guestId, session);
    }

    @GetMapping("/{chatRoomId}/message/all")
    public CursorPageResponse<ChatRoomMessageResponse> getMessages(
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "false") Boolean deleted,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit,
            @CookieValue(required = false) String guestId,
            HttpSession session) {
        return chatRoomService.getChatRoomMessages(chatRoomId, parentId, deleted, cursor, limit, guestId, session);
    }

    @PostMapping("/global/message/add")
    public MessageModel addGlobalMessage(@Valid @RequestBody ChatRoomRequest chatRoom,
            @CookieValue(required = false) String guestId, HttpSession session) {
        return chatRoomService.addChatRoomMessage(chatRoomService.getGlobalChatRoomId(), chatRoom, guestId, session);
    }

    @PostMapping("/{chatRoomId}/message/add")
    public MessageModel addMessage(@PathVariable Long chatRoomId, @Valid @RequestBody ChatRoomRequest chatRoom,
            @CookieValue(required = false) String guestId, HttpSession session) {
        return chatRoomService.addChatRoomMessage(chatRoomId, chatRoom, guestId, session);
    }

    @PatchMapping("/{chatRoomId}/message/edit")
    public MessageModel editMessage(@PathVariable Long chatRoomId, @Valid @RequestBody OptionalMessageRequest message,
            @CookieValue(required = false) String guestId, HttpSession session) {
        return chatRoomService.editChatRoomMessage(chatRoomId, message, guestId, session);
    }

    @DeleteMapping("/{chatRoomId}/message/{messageId}/delete")
    public void deleteMessage(@PathVariable Long chatRoomId, @PathVariable Long messageId,
            @CookieValue(required = false) String guestId, HttpSession session) {
        chatRoomService.deleteChatRoomMessage(chatRoomId, messageId, guestId, session);
    }
}

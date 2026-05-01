package com.applab.applab_backend.chatroom.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.applab.applab_backend.chatroom.dto.ChatRoomRequest;
import com.applab.applab_backend.chatroom.service.ChatRoomService;
import com.applab.applab_backend.message.model.MessageModel;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/chatroom")
@RequiredArgsConstructor
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    @PostMapping("/{chatRoomId}/message/add")
    public MessageModel addMessage(@PathVariable Long chatRoomId, @Valid @RequestBody ChatRoomRequest chatRoom,
            HttpSession session) {
        return chatRoomService.addChatRoomMessage(chatRoomId, chatRoom, session);
    }
}

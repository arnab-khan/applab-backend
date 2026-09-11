package com.applab.applab_backend.chatroom.dto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import lombok.Getter;

@Getter
public class ChatRoomConversationPageResponse extends PageImpl<ChatRoomConversationResponse> {
    private final long totalUnreadCount;

    public ChatRoomConversationPageResponse(Page<ChatRoomConversationResponse> page, long totalUnreadCount) {
        super(page.getContent(), page.getPageable(), page.getTotalElements());
        this.totalUnreadCount = totalUnreadCount;
    }
}

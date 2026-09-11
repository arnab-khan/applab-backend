package com.applab.applab_backend.chatroom.dto;

public record ChatRoomUnreadResponse(
        Long chatRoomId,
        long unreadCount,
        boolean otherUserHasRead) {
}

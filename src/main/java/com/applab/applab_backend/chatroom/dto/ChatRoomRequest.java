package com.applab.applab_backend.chatroom.dto;

import com.applab.applab_backend.chatroom.enums.RoomType;
import com.applab.applab_backend.chatroom.validation.ChatRoomValidation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRoomRequest implements ChatRoomValidation.OptionalNameValidation,
        ChatRoomValidation.OptionalRoomTypeValidation {
    private String name;
    private RoomType roomType;
    private Long parentId;

    @NotBlank(message = "Content is required")
    private String content;
}

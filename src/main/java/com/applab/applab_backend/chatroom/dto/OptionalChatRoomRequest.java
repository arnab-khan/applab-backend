package com.applab.applab_backend.chatroom.dto;

import com.applab.applab_backend.chatroom.enums.RoomType;
import com.applab.applab_backend.chatroom.validation.ChatRoomValidation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OptionalChatRoomRequest implements ChatRoomValidation.OptionalNameValidation, ChatRoomValidation.OptionalRoomTypeValidation {
    private String name;
    private RoomType roomType;

    @NotNull(message = "Id is required")
    private Long id;
}

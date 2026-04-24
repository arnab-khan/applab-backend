package com.applab.applab_backend.message.dto;

import com.applab.applab_backend.message.enums.RoomType;
import com.applab.applab_backend.message.validation.ChatRoomValidation;

import lombok.Data;

@Data
public class ChatRoomRequest implements ChatRoomValidation.NameValidation, ChatRoomValidation.RoomTypeValidation {
    private String name;
    private RoomType roomType;
}

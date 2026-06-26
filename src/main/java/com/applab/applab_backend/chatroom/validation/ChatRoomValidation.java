package com.applab.applab_backend.chatroom.validation;

import com.applab.applab_backend.chatroom.enums.RoomType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public interface ChatRoomValidation {
    public interface NameValidation extends OptionalNameValidation {
        @NotBlank(message = "Name is required")
        String getName();
    }

    public interface OptionalNameValidation {
        @Size(min = 1, max = 50, message = "Name must be 1-50 characters")
        String getName();
    }

    public interface RoomTypeValidation extends OptionalRoomTypeValidation {
        @NotNull(message = "Room type is required")
        RoomType getRoomType();
    }

    public interface OptionalRoomTypeValidation {
        RoomType getRoomType();
    }
}

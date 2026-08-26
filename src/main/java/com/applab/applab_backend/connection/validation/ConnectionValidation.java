package com.applab.applab_backend.connection.validation;

import com.applab.applab_backend.connection.enums.ConnectionStatus;

import jakarta.validation.constraints.NotNull;

public interface ConnectionValidation {
    public interface ReceiverUserIdValidation {
        @NotNull(message = "Receiver user id is required")
        Long getReceiverUserId();
    }

    public interface IdValidation {
        @NotNull(message = "Id is required")
        Long getId();
    }

    public interface StatusValidation {
        @NotNull(message = "Status is required")
        ConnectionStatus getStatus();
    }
}

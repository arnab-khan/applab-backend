package com.applab.applab_backend.connection.dto;

import com.applab.applab_backend.connection.validation.ConnectionValidation;

import lombok.Data;

@Data
public class ConnectionRequest implements ConnectionValidation.ReceiverUserIdValidation {
    private Long receiverUserId;
}

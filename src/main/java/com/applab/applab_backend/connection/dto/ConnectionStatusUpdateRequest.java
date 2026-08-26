package com.applab.applab_backend.connection.dto;

import com.applab.applab_backend.connection.enums.ConnectionStatus;
import com.applab.applab_backend.connection.validation.ConnectionValidation;

import lombok.Data;

@Data
public class ConnectionStatusUpdateRequest implements ConnectionValidation.IdValidation,
        ConnectionValidation.StatusValidation {
    private Long id;
    private ConnectionStatus status;
}

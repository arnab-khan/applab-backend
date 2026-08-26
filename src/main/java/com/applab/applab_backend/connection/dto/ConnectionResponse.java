package com.applab.applab_backend.connection.dto;

import com.applab.applab_backend.connection.model.ConnectionModel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConnectionResponse {
    private final ConnectionModel connection;
    private final ConnectionUserResponse user;
}

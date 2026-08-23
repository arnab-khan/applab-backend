package com.applab.applab_backend.connection.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConnectionUserResponse {
    private final Long id;
    private final String name;
    private final String username;
    private final String profileImageUrl;
    private final String compressedProfileImageUrl;
}

package com.applab.applab_backend.message.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MessageAuthorResponse {
    private final String type;
    private final Long id;
    private final String name;
    private final String username;
    private final String profileImageUrl;
    private final String compressedProfileImageUrl;
}

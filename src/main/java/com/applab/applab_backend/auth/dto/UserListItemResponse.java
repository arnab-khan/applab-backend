package com.applab.applab_backend.auth.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserListItemResponse {
    private Long id;
    private String name;
    private String username;
    private Instant createdAt;
    private Instant updatedAt;
    private String profileImageUrl;
}

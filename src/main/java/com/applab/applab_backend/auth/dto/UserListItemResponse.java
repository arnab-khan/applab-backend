package com.applab.applab_backend.auth.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserListItemResponse {
    private Long id;
    private String name;
    private String username;
    private String bio;
    private Instant createdAt;
    private Instant updatedAt;
    private String profileImageUrl;
}
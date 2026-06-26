package com.applab.applab_backend.auth.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GuestSessionResponse {
    private Long id;
    private String guestId;
    private Instant createdAt;
    private Instant updatedAt;
}

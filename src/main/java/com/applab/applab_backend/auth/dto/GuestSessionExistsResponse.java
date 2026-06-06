package com.applab.applab_backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GuestSessionExistsResponse {
    private final boolean exists;
    private final Long guestSessionId;
}

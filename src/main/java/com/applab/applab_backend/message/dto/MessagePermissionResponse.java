package com.applab.applab_backend.message.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MessagePermissionResponse {
    private final boolean canEdit;
    private final boolean canDelete;
}

package com.applab.applab_backend.chatroom.dto;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> items,
        Long nextCursor,
        boolean hasNext) {
}

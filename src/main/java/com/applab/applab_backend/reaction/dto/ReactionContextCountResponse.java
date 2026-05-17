package com.applab.applab_backend.reaction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReactionContextCountResponse {
    private final Long contextId;
    private final String emoji;
    private final Long count;
}

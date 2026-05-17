package com.applab.applab_backend.reaction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReactionCountResponse {
    private final String emoji;
    private final Long count;
}

package com.applab.applab_backend.message.dto;

import com.applab.applab_backend.message.model.MessageModel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MessageWithAuthorResponse {
    private final MessageModel message;
    private final MessageAuthorResponse author;
}

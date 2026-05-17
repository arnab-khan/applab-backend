package com.applab.applab_backend.message.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.applab.applab_backend.auth.model.UserModel;
import com.applab.applab_backend.auth.repository.UserRepository;
import com.applab.applab_backend.message.dto.MessageAuthorResponse;
import com.applab.applab_backend.message.dto.MessageWithAuthorResponse;
import com.applab.applab_backend.message.model.MessageModel;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageAuthorService {
    private final MessageService messageService;
    private final UserRepository userRepository;

    // Loads all user authors needed for this message list in one query.
    private Map<Long, UserModel> getMessageUsersById(List<MessageModel> messages) {
        return userRepository.findAllById(messages.stream()
                .map(MessageModel::getUserId)
                .filter(userId -> userId != null)
                .distinct()
                .toList())
                .stream()
                .collect(Collectors.toMap(UserModel::getId, Function.identity()));
    }

    // Builds the message author response from already-loaded users or guest data.
    private MessageAuthorResponse getMessageAuthor(MessageModel message, Map<Long, UserModel> usersById) {
        if (message.getUserId() != null) {
            UserModel user = usersById.get(message.getUserId());
            if (user == null) {
                return new MessageAuthorResponse("USER", message.getUserId(), null, null, null, null);
            }

            return new MessageAuthorResponse("USER", user.getId(), user.getName(), user.getUsername(),
                    null, user.getCompressedProfileImageUrl());
        }

        return new MessageAuthorResponse("GUEST", message.getGuestSessionId(), "Guest", null, null, null);
    }

    // Returns messages prepared for response with author details attached.
    public List<MessageWithAuthorResponse> getMessageResponsesWithAuthors(List<MessageModel> messages) {
        List<MessageModel> responseMessages = messageService.getMessagesForResponse(messages);
        Map<Long, UserModel> usersById = getMessageUsersById(responseMessages);

        return responseMessages.stream()
                .map(message -> new MessageWithAuthorResponse(message, getMessageAuthor(message, usersById)))
                .toList();
    }

}

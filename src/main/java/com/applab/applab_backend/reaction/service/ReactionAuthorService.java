package com.applab.applab_backend.reaction.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.applab.applab_backend.auth.model.UserModel;
import com.applab.applab_backend.auth.repository.UserRepository;
import com.applab.applab_backend.message.dto.MessageAuthorResponse;
import com.applab.applab_backend.reaction.model.ReactionModel;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReactionAuthorService {
    private final UserRepository userRepository;

    public List<ReactionWithAuthorResponse> getReactionResponsesWithAuthors(List<ReactionModel> reactions) {
        Map<Long, UserModel> usersById = getReactionUsersById(reactions);

        return reactions.stream()
                .map(reaction -> new ReactionWithAuthorResponse(reaction, getReactionAuthor(reaction, usersById)))
                .toList();
    }

    private Map<Long, UserModel> getReactionUsersById(List<ReactionModel> reactions) {
        return userRepository.findAllById(reactions.stream()
                .map(ReactionModel::getUserId)
                .filter(userId -> userId != null)
                .distinct()
                .toList())
                .stream()
                .collect(Collectors.toMap(UserModel::getId, Function.identity()));
    }

    private MessageAuthorResponse getReactionAuthor(ReactionModel reaction, Map<Long, UserModel> usersById) {
        if (reaction.getUserId() != null) {
            UserModel user = usersById.get(reaction.getUserId());
            if (user == null) {
                return new MessageAuthorResponse("USER", reaction.getUserId(), null, null, null, null);
            }

            return new MessageAuthorResponse("USER", user.getId(), user.getName(), user.getUsername(),
                    null, user.getCompressedProfileImageUrl());
        }

        return new MessageAuthorResponse("GUEST", reaction.getGuestSessionId(), "Guest", null, null, null);
    }

    public record ReactionWithAuthorResponse(ReactionModel reaction, MessageAuthorResponse author) {
    }
}

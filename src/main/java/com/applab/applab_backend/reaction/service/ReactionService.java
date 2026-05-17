package com.applab.applab_backend.reaction.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.applab.applab_backend.message.enums.ContextType;
import com.applab.applab_backend.reaction.dto.OptionalReactionRequest;
import com.applab.applab_backend.reaction.dto.ReactionContextCountResponse;
import com.applab.applab_backend.reaction.dto.ReactionCountResponse;
import com.applab.applab_backend.reaction.dto.ReactionRequest;
import com.applab.applab_backend.reaction.model.ReactionModel;
import com.applab.applab_backend.reaction.repository.ReactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReactionService {
    private final ReactionRepository reactionRepository;

    // ========== CRUD operations: start ==========
    // Creates a reaction row.
    public ReactionModel createReaction(ReactionRequest reaction) {
        validateUserIdOrGuestSessionId(reaction.getUserId(), reaction.getGuestSessionId());

        ReactionModel reactionModel = new ReactionModel();
        reactionModel.setContextId(reaction.getContextId());
        reactionModel.setContextType(reaction.getContextType());
        reactionModel.setUserId(reaction.getUserId());
        reactionModel.setGuestSessionId(reaction.getGuestSessionId());
        reactionModel.setEmoji(reaction.getEmoji());
        return reactionRepository.save(reactionModel);
    }

    // Updates the emoji on an existing reaction.
    public ReactionModel updateReaction(ReactionModel reaction, String emoji) {
        reaction.setEmoji(emoji);
        return reactionRepository.save(reaction);
    }

    // Updates fields on an existing reaction by id.
    public ReactionModel editReaction(OptionalReactionRequest reaction) {
        ReactionModel reactionModel = findReactionById(reaction.getId());

        if (reaction.getEmoji() != null) {
            reactionModel.setEmoji(reaction.getEmoji());
        }

        return reactionRepository.save(reactionModel);
    }

    // Permanently removes a reaction by id.
    public void deleteReaction(Long id) {
        ReactionModel reactionModel = findReactionById(id);

        reactionRepository.delete(reactionModel);
    }
    // ========== CRUD operations: end ==========

    // ========== Find operations: start ==========
    // Finds the existing reaction for the same context and same user or guest.
    public Optional<ReactionModel> findReactionByContextAndAuthor(ReactionRequest reaction) {
        if (reaction.getUserId() != null) {
            return reactionRepository.findByContextIdAndContextTypeAndUserId(reaction.getContextId(),
                    reaction.getContextType(), reaction.getUserId());
        }

        return reactionRepository.findByContextIdAndContextTypeAndGuestSessionId(reaction.getContextId(),
                reaction.getContextType(), reaction.getGuestSessionId());
    }

    // Returns all reactions for one context.
    public List<ReactionModel> getReactionsByContext(Long contextId, ContextType contextType) {
        return reactionRepository.findByContextIdAndContextTypeOrderByIdDesc(contextId, contextType);
    }

    // Finds one reaction by id or throws when it does not exist.
    public ReactionModel findReactionById(Long id) {
        if (id == null) {
            throw new RuntimeException("Reaction id is required");
        }

        return reactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reaction not found"));
    }
    // ========== Find operations: end ==========

    // ========== Count operations: start ==========
    // Returns emoji counts grouped by context id for many contexts in one query.
    public Map<Long, List<ReactionCountResponse>> getReactionCountsByContextIds(List<Long> contextIds,
            ContextType contextType) {
        if (contextIds.isEmpty()) {
            return Map.of();
        }

        return reactionRepository.countReactionsByContextIds(contextIds, contextType)
                .stream()
                .collect(Collectors.groupingBy(
                        ReactionContextCountResponse::getContextId,
                        Collectors.mapping(
                                count -> new ReactionCountResponse(count.getEmoji(), count.getCount()),
                                Collectors.toList())));
    }
    // ========== Count operations: end ==========

    // ========== Validation: start ==========
    private void validateUserIdOrGuestSessionId(Long userId, Long guestSessionId) {
        if (userId == null && guestSessionId == null) {
            throw new RuntimeException("User id or guest session id is required");
        }
    }
    // ========== Validation: end ==========

}

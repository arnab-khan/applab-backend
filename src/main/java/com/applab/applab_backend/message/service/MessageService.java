package com.applab.applab_backend.message.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.applab.applab_backend.message.dto.MessageRequest;
import com.applab.applab_backend.message.dto.OptionalMessageRequest;
import com.applab.applab_backend.message.enums.ContextType;
import com.applab.applab_backend.message.model.MessageModel;
import com.applab.applab_backend.message.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;

    public MessageModel addMessage(MessageRequest message) {
        validateUserIdOrGuestSessionId(message.getUserId(), message.getGuestSessionId());

        MessageModel messageModel = new MessageModel();
        messageModel.setParentId(message.getParentId());
        messageModel.setContextId(message.getContextId());
        messageModel.setContextType(message.getContextType());
        messageModel.setUserId(message.getUserId());
        messageModel.setGuestSessionId(message.getGuestSessionId());
        messageModel.setContent(message.getContent());
        return messageRepository.save(messageModel);
    }

    public MessageModel editMessage(OptionalMessageRequest message) {
        MessageModel messageModel = findMessageById(message.getId());

        if (message.getContent() != null) {
            messageModel.setContent(message.getContent());
            messageModel.setEdited(true);
        }

        return messageRepository.save(messageModel);
    }

    public void deleteMessage(Long id) {
        MessageModel messageModel = findMessageById(id);

        messageModel.setDeleted(true);
        messageRepository.save(messageModel);
    }

    public Page<MessageModel> getMessages(Long contextId, ContextType contextType, Long parentId, Boolean deleted,
            String keyword, Pageable pageable) {
        List<String> allowedSorts = List.of("createdAt", "updatedAt", "id");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSorts.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Invalid sort field: " + order.getProperty() +
                                ". Allowed fields: " + allowedSorts);
            }
        }

        return messageRepository.searchMessages(contextId, contextType, parentId, deleted, keyword, pageable);
    }

    public MessageModel findMessageById(Long id) {
        if (id == null) {
            throw new RuntimeException("Message id is required");
        }

        return messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
    }

    private void validateUserIdOrGuestSessionId(Long userId, Long guestSessionId) {
        if (userId == null && guestSessionId == null) {
            throw new RuntimeException("User id or guest session id is required");
        }
    }

}

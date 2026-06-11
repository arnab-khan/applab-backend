package com.applab.applab_backend.message.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.applab.applab_backend.message.dto.MessageRequest;
import com.applab.applab_backend.message.dto.EditMessageRequest;
import com.applab.applab_backend.message.enums.ContextType;
import com.applab.applab_backend.message.enums.MessageDirection;
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
        messageModel.setQuotedMessageId(message.getQuotedMessageId());
        messageModel.setContent(message.getContent());
        return messageRepository.save(messageModel);
    }

    public MessageModel editMessage(EditMessageRequest message) {
        MessageModel messageModel = findMessageById(message.getId());

        if (message.getContent() != null) {
            messageModel.setContent(message.getContent());
            messageModel.setEdited(true);
        }

        if (Boolean.TRUE.equals(message.getRemoveQuotedMessage())) {
            messageModel.setQuotedMessageId(null);
        }

        return messageRepository.save(messageModel);
    }

    public void deleteMessage(Long id) {
        MessageModel messageModel = findMessageById(id);

        messageModel.setDeleted(true);
        messageRepository.save(messageModel);
    }

    public List<MessageModel> getMessages(Long contextId, ContextType contextType, Long parentId, Boolean deleted,
            Long cursor, Long uptoId, MessageDirection direction, int limit) {
        Pageable pageable = PageRequest.of(0, limit + 1);
        if (direction == MessageDirection.NEWER) {
            return messageRepository.findNewerMessagesByCursor(contextId, contextType, parentId, deleted, cursor, uptoId,
                    pageable);
        }

        return messageRepository.findOlderMessagesByCursor(contextId, contextType, parentId, deleted, cursor, uptoId,
                pageable);
    }

    public MessageModel getMessageForResponse(MessageModel message) {
        if (message.isDeleted()) {
            message.setContent("");
        }

        return message;
    }

    public List<MessageModel> getMessagesForResponse(List<MessageModel> messages) {
        return messages.stream()
                .map(this::getMessageForResponse)
                .toList();
    }

    public List<MessageModel> findMessagesByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return messageRepository.findAllById(ids);
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

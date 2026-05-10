package com.applab.applab_backend.message.service;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public List<MessageModel> getMessages(Long contextId, ContextType contextType, Long parentId, Boolean deleted,
            Long cursor, int limit) {
        Pageable pageable = PageRequest.of(0, limit + 1);
        return messageRepository.findMessagesByCursor(contextId, contextType, parentId, deleted, cursor, pageable);
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

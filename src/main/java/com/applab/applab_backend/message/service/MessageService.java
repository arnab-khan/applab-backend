package com.applab.applab_backend.message.service;

import org.springframework.stereotype.Service;

import com.applab.applab_backend.message.dto.MessageRequest;
import com.applab.applab_backend.message.dto.OptionalMessageRequest;
import com.applab.applab_backend.message.model.MessageModel;
import com.applab.applab_backend.message.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;

    public MessageModel addMessage(MessageRequest message) {
        MessageModel messageModel = new MessageModel();
        messageModel.setParentId(message.getParentId());
        messageModel.setContextId(message.getContextId());
        messageModel.setContextType(message.getContextType());
        messageModel.setUserId(message.getUserId());
        messageModel.setContent(message.getContent());
        return messageRepository.save(messageModel);
    }

    public MessageModel editMessage(OptionalMessageRequest message) {
        MessageModel messageModel = messageRepository.findById(message.getId())
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (message.getParentId() != null) {
            messageModel.setParentId(message.getParentId());
        }
        if (message.getContextId() != null) {
            messageModel.setContextId(message.getContextId());
        }
        if (message.getContextType() != null) {
            messageModel.setContextType(message.getContextType());
        }
        if (message.getUserId() != null) {
            messageModel.setUserId(message.getUserId());
        }
        if (message.getContent() != null) {
            messageModel.setContent(message.getContent());
            messageModel.setEdited(true);
        }

        return messageRepository.save(messageModel);
    }

    public void deleteMessage(Long id) {
        MessageModel messageModel = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        messageModel.setDeleted(true);
        messageRepository.save(messageModel);
    }

}

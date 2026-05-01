package com.applab.applab_backend.chatroom.service;

import org.springframework.stereotype.Service;

import com.applab.applab_backend.chatroom.dto.ChatRoomRequest;
import com.applab.applab_backend.chatroom.enums.RoomType;
import com.applab.applab_backend.chatroom.model.ChatRoomModel;
import com.applab.applab_backend.chatroom.repository.ChatRoomRepository;
import com.applab.applab_backend.message.dto.MessageRequest;
import com.applab.applab_backend.message.enums.ContextType;
import com.applab.applab_backend.message.enums.MessageOperation;
import com.applab.applab_backend.message.model.MessageModel;
import com.applab.applab_backend.message.service.MessageService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final MessageService messageService;
    private Long globalChatRoomId;

    public ChatRoomModel createChatRoom(ChatRoomRequest chatRoom) {
        if (chatRoom.getRoomType() == RoomType.GLOBAL && chatRoomRepository.existsByRoomType(RoomType.GLOBAL)) {
            return chatRoomRepository.findByRoomType(RoomType.GLOBAL).orElse(null);
        }

        ChatRoomModel chatRoomModel = new ChatRoomModel();
        chatRoomModel.setName(chatRoom.getName());
        chatRoomModel.setRoomType(chatRoom.getRoomType());
        return chatRoomRepository.save(chatRoomModel);
    }

    public Long getGlobalChatRoomId() {
        if (globalChatRoomId != null) {
            return globalChatRoomId;
        }

        globalChatRoomId = chatRoomRepository.findByRoomType(RoomType.GLOBAL)
                .map(ChatRoomModel::getId)
                .orElse(null);

        return globalChatRoomId;
    }

    public MessageModel addChatRoomMessage(Long chatRoomId, ChatRoomRequest chatRoom, HttpSession session) {
        hasMessagePermission(MessageOperation.ADD, chatRoomId);

        MessageRequest message = new MessageRequest();
        message.setParentId(chatRoom.getParentId());
        message.setContent(chatRoom.getContent());
        message.setContextId(chatRoomId);
        message.setContextType(ContextType.CHAT);
        message.setUserId((Long) session.getAttribute("userId"));
        return messageService.addMessage(message);
    }

    public boolean hasMessagePermission(MessageOperation operation, Long chatRoomId) {
        if (operation == null) {
            throw new RuntimeException("Message operation is required");
        }

        ChatRoomModel chatRoom = findChatRoomById(chatRoomId);

        return switch (chatRoom.getRoomType()) {
            case GLOBAL -> hasGlobalRoomMessagePermission(operation);
            case GROUP -> throwNoMessagePermission();
            case PRIVATE -> throwNoMessagePermission();
        };
    }

    private ChatRoomModel findChatRoomById(Long chatRoomId) {
        if (chatRoomId == null) {
            throw new RuntimeException("Chat room id is required");
        }

        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
    }

    private boolean hasGlobalRoomMessagePermission(MessageOperation operation) {
        return switch (operation) {
            case ADD -> true;
            case EDIT -> false;
            case DELETE -> false;
        };
    }

    private boolean throwNoMessagePermission() {
        throw new RuntimeException("No permission");
    }

}

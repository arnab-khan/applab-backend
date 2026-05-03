package com.applab.applab_backend.chatroom.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.applab.applab_backend.auth.service.GuestSessionService;
import com.applab.applab_backend.chatroom.dto.ChatRoomRequest;
import com.applab.applab_backend.chatroom.enums.RoomType;
import com.applab.applab_backend.chatroom.model.ChatRoomModel;
import com.applab.applab_backend.chatroom.repository.ChatRoomRepository;
import com.applab.applab_backend.message.dto.MessageRequest;
import com.applab.applab_backend.message.dto.OptionalMessageRequest;
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
    private final GuestSessionService guestSessionService;
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

    public MessageModel addChatRoomMessage(Long chatRoomId, ChatRoomRequest chatRoom, String guestId,
            HttpSession session) {
        if (!hasMessagePermission(MessageOperation.ADD, chatRoomId, null, guestId, session)) {
            throwNoMessagePermission();
        }

        MessageRequest message = new MessageRequest();
        message.setParentId(chatRoom.getParentId());
        message.setContent(chatRoom.getContent());
        message.setContextId(chatRoomId);
        message.setContextType(ContextType.CHAT);
        Long userId = (Long) session.getAttribute("userId");
        message.setUserId(userId);
        if (userId == null) {
            message.setGuestSessionId(guestSessionService.getGuestSessionId(guestId));
        }
        return messageService.addMessage(message);
    }

    public Page<MessageModel> getChatRoomMessages(Long chatRoomId, String keyword, Long parentId, Boolean deleted,
            Pageable pageable) {
        if (!hasMessagePermission(MessageOperation.GET, chatRoomId, null, null, null)) {
            throwNoMessagePermission();
        }

        return messageService.getMessages(chatRoomId, ContextType.CHAT, parentId, deleted, keyword, pageable);
    }

    public MessageModel editChatRoomMessage(Long chatRoomId, OptionalMessageRequest message, String guestId,
            HttpSession session) {
        MessageModel savedMessage = messageService.findMessageById(message.getId());
        if (!hasMessagePermission(MessageOperation.EDIT, chatRoomId, savedMessage, guestId, session)) {
            throwNoMessagePermission();
        }

        return messageService.editMessage(message);
    }

    public void deleteChatRoomMessage(Long chatRoomId, Long messageId, String guestId, HttpSession session) {
        MessageModel message = messageService.findMessageById(messageId);
        if (!hasMessagePermission(MessageOperation.DELETE, chatRoomId, message, guestId, session)) {
            throwNoMessagePermission();
        }

        messageService.deleteMessage(messageId);
    }

    public boolean hasMessagePermission(MessageOperation operation, Long chatRoomId, MessageModel message, String guestId,
            HttpSession session) {
        if (operation == null) {
            throw new RuntimeException("Message operation is required");
        }

        ChatRoomModel chatRoom = findChatRoomById(chatRoomId);

        return switch (chatRoom.getRoomType()) {
            case GLOBAL -> hasGlobalRoomMessagePermission(operation, message, guestId, session);
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

    private boolean hasGlobalRoomMessagePermission(MessageOperation operation, MessageModel message, String guestId,
            HttpSession session) {
        return switch (operation) {
            case GET -> true;
            case ADD -> true;
            case EDIT -> isAuthor(message, guestId, session);
            case DELETE -> isAuthor(message, guestId, session);
        };
    }

    private boolean throwNoMessagePermission() {
        throw new RuntimeException("No permission");
    }

    private boolean isAuthor(MessageModel message, String guestId, HttpSession session) {
        if (message == null || session == null) {
            return false;
        }

        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            return userId.equals(message.getUserId());
        }

        Long guestSessionId = guestSessionService.getGuestSessionId(guestId);
        return guestSessionId != null && guestSessionId.equals(message.getGuestSessionId());
    }

}

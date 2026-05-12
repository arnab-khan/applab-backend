package com.applab.applab_backend.chatroom.service;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.applab.applab_backend.auth.model.UserModel;
import com.applab.applab_backend.auth.repository.UserRepository;
import com.applab.applab_backend.auth.service.GuestSessionService;
import com.applab.applab_backend.chatroom.dto.ChatRoomMessageResponse;
import com.applab.applab_backend.chatroom.dto.ChatRoomRequest;
import com.applab.applab_backend.chatroom.dto.CursorPageResponse;
import com.applab.applab_backend.chatroom.enums.RoomType;
import com.applab.applab_backend.chatroom.model.ChatRoomModel;
import com.applab.applab_backend.chatroom.repository.ChatRoomRepository;
import com.applab.applab_backend.message.dto.MessageAuthorResponse;
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
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
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

    public CursorPageResponse<ChatRoomMessageResponse> getChatRoomMessages(Long chatRoomId, Long parentId,
            Boolean deleted, Long cursor, int limit, String guestId, HttpSession session) {
        ChatRoomModel chatRoom = findChatRoomById(chatRoomId);
        if (!hasRoomMessagePermission(chatRoom.getRoomType(), MessageOperation.GET, null, null, null)) {
            throwNoMessagePermission();
        }

        MessagePermissionIdentity identity = getMessagePermissionIdentity(guestId, session);

        int normalizedLimit = Math.min(Math.max(limit, 1), 100);
        List<MessageModel> messages = messageService.getMessages(chatRoomId, ContextType.CHAT, parentId, deleted,
                cursor, normalizedLimit);
        boolean hasNext = messages.size() > normalizedLimit;
        List<MessageModel> pageMessages = hasNext ? messages.subList(0, normalizedLimit) : messages;
        Map<Long, UserModel> usersById = getMessageUsersById(messages);

        List<ChatRoomMessageResponse> items = pageMessages.stream()
                .map(message -> toChatRoomMessageResponse(chatRoomId, chatRoom.getRoomType(), message, usersById,
                        identity))
                .toList();

        Long nextCursor = hasNext && !items.isEmpty() ? items.get(items.size() - 1).getMessage().getId() : null;
        return new CursorPageResponse<>(items, nextCursor, hasNext);
    }

    public ChatRoomMessageResponse addChatRoomMessage(Long chatRoomId, ChatRoomRequest chatRoom, String guestId,
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
        MessageModel savedMessage = messageService.addMessage(message);
        ChatRoomModel chatRoomModel = findChatRoomById(chatRoomId);
        MessagePermissionIdentity identity = getMessagePermissionIdentity(guestId, session);
        Map<Long, UserModel> usersById = getMessageUsersById(List.of(savedMessage));
        ChatRoomMessageResponse response = toChatRoomMessageResponse(chatRoomId, chatRoomModel.getRoomType(),
                savedMessage, usersById, identity);
        publishChatRoomMessageToWebSocket(response);
        return response;
    }

    public ChatRoomMessageResponse editChatRoomMessage(Long chatRoomId, OptionalMessageRequest message, String guestId,
            HttpSession session) {
        MessageModel savedMessage = messageService.findMessageById(message.getId());
        if (!hasMessagePermission(MessageOperation.EDIT, chatRoomId, savedMessage, guestId, session)) {
            throwNoMessagePermission();
        }

        MessageModel editedMessage = messageService.editMessage(message);
        ChatRoomModel chatRoomModel = findChatRoomById(chatRoomId);
        MessagePermissionIdentity identity = getMessagePermissionIdentity(guestId, session);
        Map<Long, UserModel> usersById = getMessageUsersById(List.of(editedMessage));
        ChatRoomMessageResponse response = toChatRoomMessageResponse(chatRoomId, chatRoomModel.getRoomType(),
                editedMessage, usersById, identity);
        publishChatRoomMessageToWebSocket(response);
        return response;
    }

    public void deleteChatRoomMessage(Long chatRoomId, Long messageId, String guestId, HttpSession session) {
        MessageModel message = messageService.findMessageById(messageId);
        if (!hasMessagePermission(MessageOperation.DELETE, chatRoomId, message, guestId, session)) {
            throwNoMessagePermission();
        }

        messageService.deleteMessage(messageId);
    }

    public boolean hasMessagePermission(MessageOperation operation, Long chatRoomId, MessageModel message,
            String guestId,
            HttpSession session) {
        if (operation == null) {
            throw new RuntimeException("Message operation is required");
        }

        ChatRoomModel chatRoom = findChatRoomById(chatRoomId);

        MessagePermissionIdentity identity = getMessagePermissionIdentity(guestId, session);

        return hasRoomMessagePermission(chatRoom.getRoomType(), operation, message, identity.userId(),
                identity.guestSessionId());
    }

    private MessagePermissionIdentity getMessagePermissionIdentity(String guestId, HttpSession session) {
        Long userId = session != null ? (Long) session.getAttribute("userId") : null;
        Long guestSessionId = userId == null ? guestSessionService.getGuestSessionId(guestId) : null;
        return new MessagePermissionIdentity(userId, guestSessionId);
    }

    private Map<Long, UserModel> getMessageUsersById(List<MessageModel> messages) {
        return userRepository.findAllById(messages.stream()
                .map(MessageModel::getUserId)
                .filter(userId -> userId != null)
                .distinct()
                .toList())
                .stream()
                .collect(Collectors.toMap(UserModel::getId, Function.identity()));
    }

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

    private ChatRoomMessageResponse toChatRoomMessageResponse(Long chatRoomId, RoomType roomType, MessageModel message,
            Map<Long, UserModel> usersById, MessagePermissionIdentity identity) {
        return new ChatRoomMessageResponse(
                chatRoomId,
                message,
                getMessageAuthor(message, usersById),
                hasRoomMessagePermission(roomType, MessageOperation.EDIT, message, identity.userId(),
                        identity.guestSessionId()),
                hasRoomMessagePermission(roomType, MessageOperation.DELETE, message, identity.userId(),
                        identity.guestSessionId()));
    }

    private void publishChatRoomMessageToWebSocket(ChatRoomMessageResponse response) {
        messagingTemplate.convertAndSend("/topic/chatroom-message", response);
    }

    private boolean hasRoomMessagePermission(RoomType roomType, MessageOperation operation, MessageModel message,
            Long userId, Long guestSessionId) {
        return switch (roomType) {
            case GLOBAL -> hasGlobalRoomMessagePermission(operation, message, userId, guestSessionId);
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

    private boolean hasGlobalRoomMessagePermission(MessageOperation operation, MessageModel message, Long userId,
            Long guestSessionId) {
        return switch (operation) {
            case GET -> true;
            case ADD -> true;
            case EDIT -> isAuthor(message, userId, guestSessionId);
            case DELETE -> isAuthor(message, userId, guestSessionId);
        };
    }

    private boolean throwNoMessagePermission() {
        throw new RuntimeException("No permission");
    }

    private boolean isAuthor(MessageModel message, Long userId, Long guestSessionId) {
        if (message == null) {
            return false;
        }

        if (userId != null) {
            return userId.equals(message.getUserId());
        }

        return guestSessionId != null && guestSessionId.equals(message.getGuestSessionId());
    }

    private record MessagePermissionIdentity(Long userId, Long guestSessionId) {
    }

}

package com.applab.applab_backend.chatroom.service;

import java.util.List;
import java.util.Optional;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.applab.applab_backend.auth.service.GuestSessionService;
import com.applab.applab_backend.chatroom.dto.ChatRoomMessageResponse;
import com.applab.applab_backend.chatroom.dto.ChatRoomRequest;
import com.applab.applab_backend.chatroom.dto.CursorPageResponse;
import com.applab.applab_backend.chatroom.enums.RoomType;
import com.applab.applab_backend.chatroom.model.ChatRoomModel;
import com.applab.applab_backend.chatroom.repository.ChatRoomRepository;
import com.applab.applab_backend.message.dto.MessageWithAuthorAndReactionsResponse;
import com.applab.applab_backend.message.dto.MessageRequest;
import com.applab.applab_backend.message.dto.OptionalMessageRequest;
import com.applab.applab_backend.message.enums.ContextType;
import com.applab.applab_backend.message.enums.MessageOperation;
import com.applab.applab_backend.message.model.MessageModel;
import com.applab.applab_backend.message.service.MessageReactionService;
import com.applab.applab_backend.message.service.MessageService;
import com.applab.applab_backend.reaction.dto.ReactionEmojiRequest;
import com.applab.applab_backend.reaction.dto.ReactionRequest;
import com.applab.applab_backend.reaction.model.ReactionModel;
import com.applab.applab_backend.reaction.service.ReactionService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final MessageService messageService;
    private final MessageReactionService messageReactionService;
    private final ReactionService reactionService;
    private final GuestSessionService guestSessionService;
    private final SimpMessagingTemplate messagingTemplate;
    private Long globalChatRoomId;

    // ========== Chat room: start ==========
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
    // ========== Chat room: end ==========

    // ========== Messages: start ==========
    public CursorPageResponse<ChatRoomMessageResponse> getChatRoomMessages(Long chatRoomId, Long parentId,
            Boolean deleted, Long cursor, int limit, String guestId, HttpSession session) {
        ChatRoomModel chatRoom = findChatRoomById(chatRoomId);
        if (chatRoom.getRoomType() != RoomType.GLOBAL
                || !hasGlobalRoomMessagePermission(MessageOperation.GET, null, null, null, null)) {
            throwNoMessagePermission();
        }

        MessagePermissionIdentity identity = getMessagePermissionIdentity(guestId, session);

        // Keep page size between 1 and 100 so one request cannot load too many
        // messages.
        int normalizedLimit = Math.min(Math.max(limit, 1), 100);
        List<MessageModel> messages = messageService.getMessages(chatRoomId, ContextType.CHAT, parentId, deleted,
                cursor, normalizedLimit);
        // MessageService loads limit + 1 rows.
        boolean hasNext = messages.size() > normalizedLimit;
        // Only normalizedLimit rows are returned to the client.
        List<MessageModel> pageMessages = hasNext ? messages.subList(0, normalizedLimit) : messages;

        List<ChatRoomMessageResponse> items = messageReactionService
                .getMessageResponsesWithAuthorsAndReactions(pageMessages, ContextType.CHAT)
                .stream()
                .map(messageResponse -> toChatRoomMessageResponse(chatRoomId, chatRoom.getRoomType(), messageResponse,
                        identity))
                .toList();

        // The next request should start after the last returned message.
        Long nextCursor = hasNext && !items.isEmpty() ? items.get(items.size() - 1).getMessage().getId() : null;
        // CursorPageResponse includes the page items, the next cursor, and whether more
        // rows exist.
        return new CursorPageResponse<>(items, nextCursor, hasNext);
    }

    public ChatRoomMessageResponse addChatRoomMessage(Long chatRoomId, ChatRoomRequest chatRoom, String guestId,
            HttpSession session) {
        if (!hasChatRoomPermission(MessageOperation.ADD, chatRoomId, null, null, guestId, session)) {
            throwNoMessagePermission();
        }

        MessageRequest message = new MessageRequest();
        message.setParentId(chatRoom.getParentId());
        message.setQuotedMessageId(chatRoom.getQuotedMessageId());
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
        ChatRoomMessageResponse response = toChatRoomMessageResponse(chatRoomId, chatRoomModel.getRoomType(),
                messageReactionService.getMessageResponseWithAuthorAndReactions(savedMessage, ContextType.CHAT),
                identity);
        publishChatRoomMessageToWebSocket(response);
        return response;
    }

    public ChatRoomMessageResponse editChatRoomMessage(Long chatRoomId, OptionalMessageRequest message, String guestId,
            HttpSession session) {
        MessageModel savedMessage = messageService.findMessageById(message.getId());
        if (!hasChatRoomPermission(MessageOperation.EDIT, chatRoomId, savedMessage, null, guestId, session)) {
            throwNoMessagePermission();
        }

        MessageModel editedMessage = messageService.editMessage(message);
        ChatRoomModel chatRoomModel = findChatRoomById(chatRoomId);
        MessagePermissionIdentity identity = getMessagePermissionIdentity(guestId, session);
        ChatRoomMessageResponse response = toChatRoomMessageResponse(chatRoomId, chatRoomModel.getRoomType(),
                messageReactionService.getMessageResponseWithAuthorAndReactions(editedMessage, ContextType.CHAT),
                identity);
        publishChatRoomMessageToWebSocket(response);
        return response;
    }

    public void deleteChatRoomMessage(Long chatRoomId, Long messageId, String guestId, HttpSession session) {
        MessageModel message = messageService.findMessageById(messageId);
        if (!hasChatRoomPermission(MessageOperation.DELETE, chatRoomId, message, null, guestId, session)) {
            throwNoMessagePermission();
        }

        messageService.deleteMessage(messageId);
    }
    // ========== Messages: end ==========

    // ========== Reactions: start ==========
    public ReactionModel addChatRoomMessageReaction(Long messageId, ReactionEmojiRequest reaction,
            String guestId, HttpSession session) {
        MessageModel message = findChatRoomMessageById(messageId);
        Long chatRoomId = message.getContextId();

        ReactionRequest chatRoomReaction = new ReactionRequest();
        chatRoomReaction.setContextId(messageId);
        chatRoomReaction.setContextType(ContextType.CHAT);
        chatRoomReaction.setEmoji(reaction.getEmoji());

        Long userId = (Long) session.getAttribute("userId");
        chatRoomReaction.setUserId(userId);
        if (userId == null) {
            chatRoomReaction.setGuestSessionId(guestSessionService.getGuestSessionId(guestId));
        }

        Optional<ReactionModel> savedReaction = reactionService.findReactionByContextAndAuthor(chatRoomReaction);
        MessageOperation operation = getReactionOperation(reaction.getEmoji(), savedReaction);
        if (!hasChatRoomPermission(operation, chatRoomId, null, savedReaction.orElse(null), guestId, session)) {
            throwNoMessagePermission();
        }

        return switch (operation) {
            case REACTION_ADD -> reactionService.createReaction(chatRoomReaction);
            case REACTION_EDIT -> reactionService.updateReaction(savedReaction.orElseThrow(), reaction.getEmoji());
            case REACTION_DELETE -> {
                reactionService.deleteReaction(savedReaction.orElseThrow().getId());
                yield null;
            }
            default -> throw new RuntimeException("Invalid reaction operation");
        };
    }
    // ========== Reactions: end ==========

    // ========== Permissions: start ==========
    private boolean hasChatRoomPermission(MessageOperation operation, Long chatRoomId, MessageModel message,
            ReactionModel reaction, String guestId, HttpSession session) {
        if (operation == null) {
            throw new RuntimeException("Message operation is required");
        }

        ChatRoomModel chatRoom = findChatRoomById(chatRoomId);
        MessagePermissionIdentity identity = getMessagePermissionIdentity(guestId, session);

        return chatRoom.getRoomType() == RoomType.GLOBAL
                && hasGlobalRoomMessagePermission(operation, message, reaction, identity.userId(),
                        identity.guestSessionId());
    }

    private MessagePermissionIdentity getMessagePermissionIdentity(String guestId, HttpSession session) {
        Long userId = session != null ? (Long) session.getAttribute("userId") : null;
        Long guestSessionId = userId == null ? guestSessionService.getGuestSessionId(guestId) : null;
        return new MessagePermissionIdentity(userId, guestSessionId);
    }

    private boolean hasGlobalRoomMessagePermission(MessageOperation operation, MessageModel message,
            ReactionModel reaction, Long userId, Long guestSessionId) {
        return switch (operation) {
            case GET -> true;
            case ADD -> true;
            case EDIT -> message != null
                    && isAuthor(message.getUserId(), message.getGuestSessionId(), userId, guestSessionId);
            case DELETE -> message != null
                    && isAuthor(message.getUserId(), message.getGuestSessionId(), userId, guestSessionId);
            case REACTION_ADD -> true;
            case REACTION_EDIT -> reaction != null
                    && isAuthor(reaction.getUserId(), reaction.getGuestSessionId(), userId, guestSessionId);
            case REACTION_DELETE -> reaction != null
                    && isAuthor(reaction.getUserId(), reaction.getGuestSessionId(), userId, guestSessionId);
        };
    }

    private boolean throwNoMessagePermission() {
        throw new RuntimeException("No permission");
    }

    private boolean isAuthor(Long authorUserId, Long authorGuestSessionId, Long userId, Long guestSessionId) {
        if (userId != null) {
            return userId.equals(authorUserId);
        }

        return guestSessionId != null && guestSessionId.equals(authorGuestSessionId);
    }

    private MessageOperation getReactionOperation(String emoji, Optional<ReactionModel> savedReaction) {
        if (emoji.isEmpty()) {
            return MessageOperation.REACTION_DELETE;
        }

        return savedReaction.isPresent() ? MessageOperation.REACTION_EDIT : MessageOperation.REACTION_ADD;
    }
    // ========== Permissions: end ==========

    // ========== Response and publishing: start ==========
    private ChatRoomMessageResponse toChatRoomMessageResponse(Long chatRoomId, RoomType roomType,
            MessageWithAuthorAndReactionsResponse messageResponse, MessagePermissionIdentity identity) {
        return new ChatRoomMessageResponse(
                chatRoomId,
                messageResponse.getMessage(),
                messageResponse.getAuthor(),
                messageResponse.getReactions(),
                roomType == RoomType.GLOBAL
                        && hasGlobalRoomMessagePermission(MessageOperation.EDIT, messageResponse.getMessage(), null,
                                identity.userId(), identity.guestSessionId()),
                roomType == RoomType.GLOBAL
                        && hasGlobalRoomMessagePermission(MessageOperation.DELETE, messageResponse.getMessage(), null,
                                identity.userId(), identity.guestSessionId()));
    }

    private void publishChatRoomMessageToWebSocket(ChatRoomMessageResponse response) {
        messagingTemplate.convertAndSend("/topic/chatroom-message", response);
    }
    // ========== Response and publishing: end ==========

    // ========== Lookups: start ==========
    private ChatRoomModel findChatRoomById(Long chatRoomId) {
        if (chatRoomId == null) {
            throw new RuntimeException("Chat room id is required");
        }

        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
    }

    private MessageModel findChatRoomMessageById(Long messageId) {
        MessageModel message = messageService.findMessageById(messageId);
        if (message.getContextType() != ContextType.CHAT) {
            throw new RuntimeException("Message not found");
        }

        return message;
    }
    // ========== Lookups: end ==========

    private record MessagePermissionIdentity(Long userId, Long guestSessionId) {
    }

}

package com.applab.applab_backend.chatroom.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.applab.applab_backend.auth.service.GuestSessionService;
import com.applab.applab_backend.chatroom.dto.ChatRoomMessageResponse;
import com.applab.applab_backend.chatroom.dto.ChatRoomReactionPageResponse;
import com.applab.applab_backend.chatroom.dto.ChatRoomRequest;
import com.applab.applab_backend.chatroom.dto.CursorPageResponse;
import com.applab.applab_backend.chatroom.enums.RoomType;
import com.applab.applab_backend.chatroom.model.ChatRoomModel;
import com.applab.applab_backend.chatroom.repository.ChatRoomRepository;
import com.applab.applab_backend.message.dto.MessageRequest;
import com.applab.applab_backend.message.dto.MessageWithAuthorAndReactionsResponse;
import com.applab.applab_backend.message.dto.MessageWithAuthorResponse;
import com.applab.applab_backend.message.dto.OptionalMessageRequest;
import com.applab.applab_backend.message.enums.ContextType;
import com.applab.applab_backend.message.enums.MessageOperation;
import com.applab.applab_backend.message.model.MessageModel;
import com.applab.applab_backend.message.service.MessageAuthorService;
import com.applab.applab_backend.message.service.MessageReactionService;
import com.applab.applab_backend.message.service.MessageService;
import com.applab.applab_backend.reaction.dto.ReactionEmojiRequest;
import com.applab.applab_backend.reaction.dto.ReactionCountResponse;
import com.applab.applab_backend.reaction.dto.ReactionRequest;
import com.applab.applab_backend.reaction.model.ReactionModel;
import com.applab.applab_backend.reaction.service.ReactionAuthorService;
import com.applab.applab_backend.reaction.service.ReactionAuthorService.ReactionWithAuthorResponse;
import com.applab.applab_backend.reaction.service.ReactionService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final MessageService messageService;
    private final MessageAuthorService messageAuthorService;
    private final MessageReactionService messageReactionService;
    private final ReactionService reactionService;
    private final GuestSessionService guestSessionService;
    private final ReactionAuthorService reactionAuthorService;
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
        requireChatRoomPermission(MessageOperation.GET, chatRoom.getRoomType(), null, null,
                new MessagePermissionIdentity(null, null));

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

        List<MessageWithAuthorAndReactionsResponse> messageResponses = messageReactionService
                .getMessageResponsesWithAuthorsAndReactions(pageMessages, ContextType.CHAT, identity.userId(),
                        identity.guestSessionId());
        Map<Long, MessageWithAuthorResponse> quotedMessagesById = getQuotedMessagesById(pageMessages);

        List<ChatRoomMessageResponse> items = messageResponses.stream()
                .map(messageResponse -> toChatRoomMessageResponse(chatRoomId, chatRoom.getRoomType(), messageResponse,
                        quotedMessagesById.get(messageResponse.getMessage().getQuotedMessageId()), identity))
                .toList();

        // The next request should start after the last returned message.
        Long nextCursor = hasNext && !items.isEmpty() ? items.get(items.size() - 1).getMessage().getId() : null;
        // CursorPageResponse includes the page items, the next cursor, and whether more
        // rows exist.
        return new CursorPageResponse<>(items, nextCursor, hasNext);
    }

    public ChatRoomMessageResponse addChatRoomMessage(Long chatRoomId, ChatRoomRequest chatRoom, String guestId,
            HttpSession session) {
        ChatRoomModel chatRoomModel = findChatRoomById(chatRoomId);
        MessagePermissionIdentity identity = getMessagePermissionIdentity(guestId, session);
        requireChatRoomPermission(MessageOperation.ADD, chatRoomModel.getRoomType(), null, null, identity);

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
        ChatRoomMessageResponse response = toChatRoomMessageResponse(chatRoomId, chatRoomModel.getRoomType(),
                messageReactionService.getMessageResponseWithAuthorAndReactions(savedMessage, ContextType.CHAT,
                        identity.userId(), identity.guestSessionId()),
                getQuotedMessage(savedMessage), identity);
        publishChatRoomMessageToWebSocket(response);
        return response;
    }

    public ChatRoomMessageResponse editChatRoomMessage(Long chatRoomId, OptionalMessageRequest message, String guestId,
            HttpSession session) {
        MessageModel savedMessage = messageService.findMessageById(message.getId());
        ChatRoomModel chatRoomModel = findChatRoomById(chatRoomId);
        MessagePermissionIdentity identity = getMessagePermissionIdentity(guestId, session);
        requireChatRoomPermission(MessageOperation.EDIT, chatRoomModel.getRoomType(), savedMessage, null, identity);

        MessageModel editedMessage = messageService.editMessage(message);
        ChatRoomMessageResponse response = toChatRoomMessageResponse(chatRoomId, chatRoomModel.getRoomType(),
                messageReactionService.getMessageResponseWithAuthorAndReactions(editedMessage, ContextType.CHAT,
                        identity.userId(), identity.guestSessionId()),
                getQuotedMessage(editedMessage), identity);
        publishChatRoomMessageToWebSocket(response);
        return response;
    }

    public void deleteChatRoomMessage(Long chatRoomId, Long messageId, String guestId, HttpSession session) {
        MessageModel message = messageService.findMessageById(messageId);
        ChatRoomModel chatRoomModel = findChatRoomById(chatRoomId);
        MessagePermissionIdentity identity = getMessagePermissionIdentity(guestId, session);
        requireChatRoomPermission(MessageOperation.DELETE, chatRoomModel.getRoomType(), message, null, identity);

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
        ChatRoomModel chatRoomModel = findChatRoomById(chatRoomId);
        MessagePermissionIdentity identity = getMessagePermissionIdentity(guestId, session);
        requireChatRoomPermission(operation, chatRoomModel.getRoomType(), null, savedReaction.orElse(null), identity);

        ReactionModel updatedReaction = switch (operation) {
            case REACTION_ADD -> reactionService.createReaction(chatRoomReaction);
            case REACTION_EDIT -> reactionService.updateReaction(savedReaction.orElseThrow(), reaction.getEmoji());
            case REACTION_DELETE -> {
                reactionService.deleteReaction(savedReaction.orElseThrow().getId());
                yield null;
            }
            default -> throw new RuntimeException("Invalid reaction operation");
        };

        ChatRoomMessageResponse response = toChatRoomMessageResponse(chatRoomId, chatRoomModel.getRoomType(),
                messageReactionService.getMessageResponseWithAuthorAndReactions(message, ContextType.CHAT,
                        identity.userId(), identity.guestSessionId()),
                getQuotedMessage(message), identity);
        publishChatRoomMessageToWebSocket(response);

        return updatedReaction;
    }

    public ChatRoomReactionPageResponse getChatRoomMessageReactions(Long chatRoomId, Long messageId, String emoji,
            Long cursor, int limit, String guestId, HttpSession session) {
        MessageModel message = findChatRoomMessageById(messageId);
        ChatRoomModel chatRoomModel = findChatRoomById(chatRoomId);
        MessagePermissionIdentity identity = getMessagePermissionIdentity(guestId, session);
        if (!chatRoomId.equals(message.getContextId())) {
            throwNoMessagePermission();
        }
        requireChatRoomPermission(MessageOperation.GET, chatRoomModel.getRoomType(), message, null, identity);

        int normalizedLimit = Math.min(Math.max(limit, 1), 100);
        List<ReactionModel> reactions = reactionService.getReactionsByContext(messageId, ContextType.CHAT, emoji, cursor,
                normalizedLimit);
        boolean hasNext = reactions.size() > normalizedLimit;
        List<ReactionModel> pageReactions = hasNext ? reactions.subList(0, normalizedLimit) : reactions;

        List<ReactionWithAuthorResponse> items = reactionAuthorService.getReactionResponsesWithAuthors(pageReactions);

        Long nextCursor = hasNext && !items.isEmpty() ? items.get(items.size() - 1).reaction().getId() : null;
        List<ReactionCountResponse> reactionCounts = reactionService
                .getReactionCountsByContextIds(List.of(messageId), ContextType.CHAT)
                .getOrDefault(messageId, List.of());
        return new ChatRoomReactionPageResponse(items, nextCursor, hasNext, reactionCounts);
    }
    // ========== Reactions: end ==========

    // ========== Permissions: start ==========
    private boolean hasChatRoomPermission(MessageOperation operation, RoomType roomType, MessageModel message,
            ReactionModel reaction, MessagePermissionIdentity identity) {
        if (operation == null) {
            throw new RuntimeException("Message operation is required");
        }

        return switch (roomType) {
            case GLOBAL -> hasGlobalRoomMessagePermission(operation, message, reaction, identity.userId(),
                    identity.guestSessionId());
            default -> throw new RuntimeException("Unsupported chat room type");
        };
    }

    private void requireChatRoomPermission(MessageOperation operation, RoomType roomType, MessageModel message,
            ReactionModel reaction, MessagePermissionIdentity identity) {
        if (!hasChatRoomPermission(operation, roomType, message, reaction, identity)) {
            throwNoMessagePermission();
        }
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

    private ChatRoomMessageResponse toChatRoomMessageResponse(Long chatRoomId, RoomType roomType,
            MessageWithAuthorAndReactionsResponse messageResponse, MessageWithAuthorResponse quotedMessage,
            MessagePermissionIdentity identity) {
        return new ChatRoomMessageResponse(
                chatRoomId,
                messageResponse.getMessage(),
                messageResponse.getAuthor(),
                quotedMessage,
                messageResponse.getReactions(),
                messageResponse.getMyReaction(),
                hasChatRoomPermission(MessageOperation.EDIT, roomType, messageResponse.getMessage(), null, identity),
                hasChatRoomPermission(MessageOperation.DELETE, roomType, messageResponse.getMessage(), null, identity));
    }

    private Map<Long, MessageWithAuthorResponse> getQuotedMessagesById(List<MessageModel> messages) {
        List<Long> quotedMessageIds = messages.stream()
                .map(MessageModel::getQuotedMessageId)
                .filter(quotedMessageId -> quotedMessageId != null)
                .distinct()
                .toList();

        return messageAuthorService.getMessageResponsesWithAuthors(messageService.findMessagesByIds(quotedMessageIds))
                .stream()
                .collect(Collectors.toMap(
                        messageResponse -> messageResponse.getMessage().getId(),
                        Function.identity()));
    }

    private MessageWithAuthorResponse getQuotedMessage(MessageModel message) {
        if (message.getQuotedMessageId() == null) {
            return null;
        }

        return getQuotedMessagesById(List.of(message)).get(message.getQuotedMessageId());
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

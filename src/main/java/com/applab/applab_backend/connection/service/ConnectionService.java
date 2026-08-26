package com.applab.applab_backend.connection.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.applab.applab_backend.auth.model.UserModel;
import com.applab.applab_backend.auth.repository.UserRepository;
import com.applab.applab_backend.connection.dto.ConnectionResponse;
import com.applab.applab_backend.connection.dto.ConnectionUserResponse;
import com.applab.applab_backend.connection.enums.ConnectionStatus;
import com.applab.applab_backend.connection.model.ConnectionModel;
import com.applab.applab_backend.connection.repository.ConnectionRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;

    public ConnectionService(ConnectionRepository connectionRepository, UserRepository userRepository) {
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
    }

    public ConnectionModel createConnection(Long receiverUserId, HttpSession session) {
        Long senderUserId = (Long) session.getAttribute("userId");
        System.out.println("senderUserId=" + senderUserId + ", receiverUserId=" + receiverUserId);
        if (senderUserId.equals(receiverUserId)) {
            throw new RuntimeException("You cannot connect with yourself");
        }
        if (connectionRepository.existsConnectionBetweenUsers(senderUserId, receiverUserId)) {
            throw new RuntimeException("Connection request already sent");
        }

        ConnectionModel connection = new ConnectionModel();
        connection.setSenderUserId(senderUserId);
        connection.setReceiverUserId(receiverUserId);
        connection.setStatus(ConnectionStatus.PENDING);
        return connectionRepository.save(connection);
    }

    public Optional<ConnectionModel> getConnectionStatus(Long userId, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("userId");
        return connectionRepository.findConnectionBetweenUsers(currentUserId, userId);
    }

    public ConnectionModel updateConnectionStatus(Long id, ConnectionStatus status, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        ConnectionModel connection = connectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        if (connection.getSenderUserId().equals(userId)) {
            boolean canCancel = (connection.getStatus() == ConnectionStatus.PENDING
                    || connection.getStatus() == ConnectionStatus.ACCEPTED)
                    && status == ConnectionStatus.CANCELED;
            boolean canResend = (connection.getStatus() == ConnectionStatus.REJECTED
                    || connection.getStatus() == ConnectionStatus.CANCELED)
                    && status == ConnectionStatus.PENDING;
            if (!canCancel && !canResend) {
                throw new RuntimeException("Sender can cancel pending or accepted connections, or retry requests");
            }
        } else if (connection.getReceiverUserId().equals(userId)) {
            boolean canAccept = status == ConnectionStatus.ACCEPTED
                    && (connection.getStatus() == ConnectionStatus.PENDING
                            || connection.getStatus() == ConnectionStatus.REJECTED);
            boolean canReject = status == ConnectionStatus.REJECTED
                    && connection.getStatus() == ConnectionStatus.PENDING;
            boolean canResend = status == ConnectionStatus.PENDING
                    && connection.getStatus() == ConnectionStatus.CANCELED;
            boolean canCancel = status == ConnectionStatus.CANCELED
                    && connection.getStatus() == ConnectionStatus.ACCEPTED;
            if (!canAccept && !canReject && !canResend && !canCancel) {
                throw new RuntimeException(
                        "Receiver can accept, reject, retry canceled requests, or cancel accepted connections");
            }
        } else {
            throw new RuntimeException("Unauthorized, you can only update your own connections");
        }

        if (connection.getStatus() == ConnectionStatus.CANCELED && status == ConnectionStatus.PENDING) {
            Long otherUserId = connection.getSenderUserId().equals(userId)
                    ? connection.getReceiverUserId()
                    : connection.getSenderUserId();
            connection.setSenderUserId(userId);
            connection.setReceiverUserId(otherUserId);
        }

        connection.setStatus(status);
        return connectionRepository.save(connection);
    }

    public Page<ConnectionResponse> getConnections(Long userId, ConnectionStatus status, String keyword, String sortBy,
            String sortDirection, Pageable pageable, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("userId");
        if (userId == null) {
            if (currentUserId == null) {
                throw new IllegalArgumentException("userId is required when not logged in");
            }
            userId = currentUserId;
        } else if (!userId.equals(currentUserId)) {
            if (status != null && status != ConnectionStatus.ACCEPTED) {
                throw new RuntimeException("You can only view accepted connections of another user");
            }
            status = ConnectionStatus.ACCEPTED;
        }

        List<String> allowedSorts = List.of("updatedAt", "name");
        if (!allowedSorts.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sort field: " + sortBy +
                            ". Allowed fields: " + allowedSorts);
        }

        List<String> allowedSortDirections = List.of("asc", "desc");
        if (!allowedSortDirections.contains(sortDirection)) {
            throw new IllegalArgumentException(
                    "Invalid sort direction: " + sortDirection +
                            ". Allowed directions: " + allowedSortDirections);
        }

        Long requestedUserId = userId;
        Page<ConnectionModel> connections = connectionRepository.searchConnections(
                requestedUserId, status, keyword, sortBy, sortDirection, pageable);

        Map<Long, UserModel> usersById = userRepository.findAllById(connections.getContent().stream()
                .map(connection -> connection.getSenderUserId().equals(requestedUserId)
                        ? connection.getReceiverUserId()
                        : connection.getSenderUserId())
                .distinct()
                .toList())
                .stream()
                .collect(Collectors.toMap(UserModel::getId, Function.identity()));

        return connections.map(connection -> {
            Long otherUserId = connection.getSenderUserId().equals(requestedUserId)
                    ? connection.getReceiverUserId()
                    : connection.getSenderUserId();
            UserModel user = usersById.get(otherUserId);
            ConnectionUserResponse userResponse = user == null
                    ? new ConnectionUserResponse(otherUserId, null, null, null, null)
                    : new ConnectionUserResponse(user.getId(), user.getName(), user.getUsername(),
                            user.getProfileImageUrl(), user.getCompressedProfileImageUrl());
            return new ConnectionResponse(connection, userResponse);
        });
    }
}

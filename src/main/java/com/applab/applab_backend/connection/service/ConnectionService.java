package com.applab.applab_backend.connection.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.applab.applab_backend.connection.enums.ConnectionStatus;
import com.applab.applab_backend.connection.model.ConnectionModel;
import com.applab.applab_backend.connection.repository.ConnectionRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class ConnectionService {

    private final ConnectionRepository connectionRepository;

    public ConnectionService(ConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
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

    public Optional<ConnectionStatus> getConnectionStatus(Long userId, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("userId");
        return connectionRepository.findConnectionBetweenUsers(currentUserId, userId)
                .map(connection -> connection.getStatus());
    }

    public ConnectionModel updateConnectionStatus(Long id, ConnectionStatus status, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        ConnectionModel connection = connectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        if (connection.getSenderUserId().equals(userId)) {
            if (status != ConnectionStatus.CANCELED) {
                throw new RuntimeException("Sender can only cancel connection request");
            }
        } else if (connection.getReceiverUserId().equals(userId)) {
            if (status != ConnectionStatus.ACCEPTED && status != ConnectionStatus.REJECTED) {
                throw new RuntimeException("Receiver can only accept or reject connection request");
            }
        } else {
            throw new RuntimeException("Unauthorized, you can only update your own connections");
        }

        connection.setStatus(status);
        return connectionRepository.save(connection);
    }

    public Page<ConnectionModel> getConnections(Long userId, ConnectionStatus status, String keyword, String sortBy,
            String sortDirection, Pageable pageable, HttpSession session) {
        if (userId == null) {
            userId = (Long) session.getAttribute("userId");
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

        return connectionRepository.searchConnections(userId, status, keyword, sortBy, sortDirection, pageable);
    }
}

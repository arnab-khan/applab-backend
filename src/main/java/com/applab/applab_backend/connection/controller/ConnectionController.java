package com.applab.applab_backend.connection.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.applab.applab_backend.connection.dto.ConnectionRequest;
import com.applab.applab_backend.connection.dto.ConnectionResponse;
import com.applab.applab_backend.connection.dto.ConnectionStatusUpdateRequest;
import com.applab.applab_backend.connection.enums.ConnectionStatus;
import com.applab.applab_backend.connection.model.ConnectionModel;
import com.applab.applab_backend.connection.service.ConnectionService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/connection")
public class ConnectionController {

    private final ConnectionService connectionService;

    public ConnectionController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @GetMapping("/all")
    public Page<ConnectionResponse> getAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) ConnectionStatus status,
            Pageable pageable,
            HttpSession session) {
        Sort.Order sortOrder = pageable.getSort().stream().findFirst()
                .orElse(Sort.Order.desc("updatedAt"));
        String sortBy = sortOrder.getProperty();
        String sortDirection = sortOrder.getDirection().name().toLowerCase();
        return connectionService.getConnections(userId, status, null, sortBy, sortDirection, pageable, session);
    }

    @PostMapping("/add")
    public ConnectionModel createConnection(@Valid @RequestBody ConnectionRequest connectionRequest,
            HttpSession session) {
        return connectionService.createConnection(
                connectionRequest.getReceiverUserId(),
                session);
    }

    @GetMapping("/status")
    public ResponseEntity<ConnectionModel> getConnectionStatus(@RequestParam Long userId, HttpSession session) {
        return connectionService.getConnectionStatus(userId, session)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PatchMapping("/status")
    public ConnectionModel updateConnectionStatus(@Valid @RequestBody ConnectionStatusUpdateRequest statusRequest,
            HttpSession session) {
        return connectionService.updateConnectionStatus(
                statusRequest.getId(),
                statusRequest.getStatus(),
                session);
    }
}

package com.applab.applab_backend.ai.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ai_chats", indexes = {
        @Index(name = "idx_ai_chats_session_id", columnList = "ai_session_id, id")
})
public class AiChatModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String aiSessionId;

    @Column(nullable = false, length = 50)
    private String aiModel;

    private Long userId;

    @Column(nullable = false, length = 250)
    private String userMessage;

    @Column(nullable = false, length = 3000)
    private String assistantResponse;

    @Column(nullable = false, length = 1500)
    private String historyResponse;

    @Column(length = 500)
    private String currentRoute;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;
}

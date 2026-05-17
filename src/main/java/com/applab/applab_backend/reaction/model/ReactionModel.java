package com.applab.applab_backend.reaction.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.applab.applab_backend.message.enums.ContextType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "reactions", indexes = {
        @Index(name = "idx_reactions_context_type_context_id_emoji", columnList = "context_type, context_id, emoji")
})
public class ReactionModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long contextId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContextType contextType;

    private Long userId;

    private Long guestSessionId;

    @Column(nullable = false)
    private String emoji;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;
}

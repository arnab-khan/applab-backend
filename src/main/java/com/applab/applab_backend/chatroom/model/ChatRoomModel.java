package com.applab.applab_backend.chatroom.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.applab.applab_backend.chatroom.enums.RoomType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chat_rooms", uniqueConstraints = @UniqueConstraint(name = "uk_direct_chat_users", columnNames = {"first_user_id", "second_user_id"}), indexes = {
        @Index(name = "idx_chat_rooms_room_type_updated_at", columnList = "room_type, updated_at")
})
public class ChatRoomModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType roomType;

    @Column(name = "first_user_id", updatable = false)
    private Long firstUserId;

    @Column(name = "second_user_id", updatable = false)
    private Long secondUserId;

    @JsonIgnore
    @Column(name = "first_user_unread_count", nullable = false)
    private long firstUserUnreadCount;

    @JsonIgnore
    @Column(name = "second_user_unread_count", nullable = false)
    private long secondUserUnreadCount;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}

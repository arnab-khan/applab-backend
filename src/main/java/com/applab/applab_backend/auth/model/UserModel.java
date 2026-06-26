package com.applab.applab_backend.auth.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.applab.applab_backend.common.views.SerializationJsonViews;
import com.applab.applab_backend.storage.model.FileEntityModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_name", columnList = "name"),
        @Index(name = "idx_users_created_at", columnList = "created_at")
})
public class UserModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(SerializationJsonViews.MyClass.class) // Visible in public view
    private Long id;

    @JsonView(SerializationJsonViews.MyClass.class) // Visible in public view
    private String name;

    @JsonView(SerializationJsonViews.MyClass.class) // Visible in public view
    private String bio;

    @Column(nullable = false, unique = true)
    @JsonView(SerializationJsonViews.MyClass.class) // Visible in public view
    private String username;

    @Column(nullable = false)
    @JsonView(SerializationJsonViews.MyChild.class) // Visible only in internal view
    private String password;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    @JsonView(SerializationJsonViews.MyClass.class) // Visible in public view
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    @JsonView(SerializationJsonViews.MyClass.class) // Visible in public view
    private Instant updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_image_id")
    @JsonIgnore
    private FileEntityModel profileImage;

    public String getProfileImageUrl() {
        return profileImage != null ? "/user/public/profile-image/raw/" + id : null;
    }

    public String getCompressedProfileImageUrl() {
        return profileImage != null ? "/user/public/profile-image/compressed/" + id : null;
    }
}

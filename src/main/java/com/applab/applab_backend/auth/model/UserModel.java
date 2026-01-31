package com.applab.applab_backend.auth.model;

import com.applab.applab_backend.common.views.SerializationJsonViews;
import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class UserModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(SerializationJsonViews.MyClass.class) // Visible in public view
    private Long id;

    @JsonView(SerializationJsonViews.MyClass.class) // Visible in public view
    private String name;

    @Column(nullable = false, unique = true)
    @JsonView(SerializationJsonViews.MyClass.class) // Visible in public view
    private String username;

    @Column(nullable = false)
    @JsonView(SerializationJsonViews.MyChild.class) // Visible only in internal view
    private String password;
}

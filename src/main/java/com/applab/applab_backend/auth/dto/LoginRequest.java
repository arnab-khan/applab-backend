package com.applab.applab_backend.auth.dto;

import com.applab.applab_backend.auth.validation.UserValidation;

import lombok.Data;

@Data
public class LoginRequest implements
        UserValidation.RequiredUserNameValidetion,
        UserValidation.RequiredPasswordValidation {

    private String username;
    private String password;

}

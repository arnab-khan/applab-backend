package com.applab.applab_backend.auth.dto;

import com.applab.applab_backend.auth.validation.UserValidation;

import lombok.Data;

@Data
public class ProfileCredentialsUpdateRequest implements
        UserValidation.OptionalUsernameValidation,
        UserValidation.OptionalPasswordValidation,
        UserValidation.CurrentPasswordValidation {

    private String username;

    private String password;

    private String currentPassword;
}

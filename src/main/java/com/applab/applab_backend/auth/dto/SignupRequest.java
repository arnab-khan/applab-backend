package com.applab.applab_backend.auth.dto;

import com.applab.applab_backend.auth.validation.UserValidation;
import lombok.Data;

@Data
public class SignupRequest implements
        UserValidation.NameValidation,
        UserValidation.UsernameValidation,
        UserValidation.PasswordValidation {

    private String name;
    private String username;
    private String password;
}
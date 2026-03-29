package com.applab.applab_backend.auth.dto;

import com.applab.applab_backend.auth.validation.UserValidation;

import lombok.Data;

@Data
public class ProfileBasicsUpdateRequest implements
        UserValidation.OptionalNameValidation,
        UserValidation.OptionalBioValidation {

    private String name;
    private String bio;
}

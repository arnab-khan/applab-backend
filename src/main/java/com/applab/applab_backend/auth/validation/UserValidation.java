package com.applab.applab_backend.auth.validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public interface UserValidation {
    public interface NameValidation extends OptionalNameValidation {
        @NotBlank(message = "Name is required")
        String getName();
    }

    public interface OptionalNameValidation {
        @Size(min = 1, max = 50, message = "Name must be 1-50 characters")
        String getName();
    }

    public interface UsernameValidation extends OptionalUsernameValidation, RequiredUserNameValidetion {
        String getUsername();
    }

    public interface OptionalUsernameValidation {
        @Size(min = 1, max = 20, message = "Username must be 1-20 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores, and hyphens")
        String getUsername();
    }

    public interface RequiredUserNameValidetion {
        @NotBlank(message = "Username is required")
        String getUsername();
    }

    public interface PasswordValidation extends RequiredPasswordValidation {
        @Size(min = 6, max = 100, message = "Password must be at least 6 characters long and up to 100 characters")
        @Pattern(regexp = "^[^\\s]+$", message = "Password cannot contain spaces")
        String getPassword();
    }

    public interface RequiredPasswordValidation {
        @NotBlank(message = "Password is required")
        String getPassword();
    }
}

package com.example.user_service.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@Data
public class CreateUserRequest {
    @NotBlank(message = "name is null/empty")
    private String name;

    @NotBlank(message = "email is null/empty")
    @Size(max = 255)
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "must be a valid email address"
    )
    @JsonDeserialize
    private String email;

    @Min(1)
    @Max(3)
    @NotNull
    private Integer tier;
}

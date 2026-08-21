package com.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data //getter & setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @NotBlank(message = "userId is null/empty")
    private UUID userId;

    @NotBlank(message = "name is null/empty")
    private String name;

    @NotBlank(message = "email is null/empty")
    @Size(max = 255)
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "must be a valid email address"
    )
    private String email;

    @Min(1)
    @Max(3)
    private Integer tier;

    private OffsetDateTime createdAt;


}

package com.example.user_service.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data //getter & setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    private UUID userId;

    private String name;

    private String email;

    private Integer tier;

    private OffsetDateTime createdAt;


}

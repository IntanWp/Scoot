package com.example.user_service.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateTierRequest {
    @NotBlank(message = "userId is null/empty")
    private UUID userId;

    @Min(1)
    @Max(3)
    private Integer tier;
}

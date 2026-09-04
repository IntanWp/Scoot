package com.example.user_service.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateTierRequest {
    @Min(1)
    @Max(3)
    @NotNull
    private Integer tier;
}

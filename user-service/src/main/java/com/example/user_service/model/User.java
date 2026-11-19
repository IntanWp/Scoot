package com.example.user_service.model;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data //getter & setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("users")
public class User {
    private String userId;

    private String name;

    private String email;

    private Integer tier;

    private OffsetDateTime createdAt;


}

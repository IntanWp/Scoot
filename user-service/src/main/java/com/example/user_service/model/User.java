package com.example.user_service.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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
    @TableId(value = "user_id", type = IdType.ASSIGN_UUID)
    private String userId;

    private String name;

    private String email;

    private Integer tier;

    @TableField("created_at")
    private OffsetDateTime createdAt;


}

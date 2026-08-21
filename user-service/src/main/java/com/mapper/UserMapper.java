package com.mapper;

import com.model.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Mapper
public interface UserMapper {
    @Insert("""
        INSERT INTO users (user_id, name, email, tier)
        VALUES (#{userId}, #{name}, #{email}, #{tier})
        """)
    void insert(User user);

    @Select("SELECT * FROM users WHERE user_id = #{userId}")
    User findById(UUID userId);

    @Select("SELECT * FROM users ORDER BY created_at")
    List<User> findAll();

    @Update("UPDATE users SET tier = #{tier} WHERE user_id = #{userId}")
    int updateTier(@Param("userId") UUID userId, @Param("tier") Integer tier);
}

package com.example.user_service.mapper;

import com.example.user_service.model.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    @Insert("""
        INSERT INTO users (user_id, name, email, tier, password_hash)
        VALUES (#{userId}::uuid, #{name}, #{email}, #{tier}, #{password})
        """)
    void insert(User user);

    @Select("SELECT * FROM users WHERE user_id = #{userId}::uuid")
    User findById(String userId);

    @Select("SELECT * FROM users ORDER BY created_at")
    List<User> findAll();

    @Update("UPDATE users SET tier = #{tier} WHERE user_id = #{userId}::uuid")
    int updateTier(@Param("userId") String userId, @Param("tier") Integer tier);
}

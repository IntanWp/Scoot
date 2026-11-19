package com.example.user_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.user_service.model.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}

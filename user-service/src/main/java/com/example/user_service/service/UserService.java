package com.example.user_service.service;

import com.example.user_service.exception.ErrorCodeEnum;
import com.example.user_service.exception.UserException;
import com.example.user_service.mapper.UserMapper;
import com.example.user_service.model.User;
import com.example.user_service.model.dto.request.CreateUserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public List<User> getAllUsers(){
        return userMapper.findAll();
    }

    public User createUser(CreateUserRequest request){
        User newUser = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .tier(request.getTier())
                .name(request.getName())
                .userId(UUID.randomUUID().toString())
                .password(request.getPassword())
                .build();

        try {
            userMapper.insert(newUser);
        } catch (DuplicateKeyException e) {
            throw new UserException(ErrorCodeEnum.EMAIL_ALREADY_EXISTS);
        }
        return userMapper.findById(newUser.getUserId());

    }

    public User findById(String id){

        User user = userMapper.findById(id);
        if (user == null) throw new UserException(ErrorCodeEnum.USER_NOT_FOUND);
        return user;

    }

    public User updateTier(String id, Integer newTier){

        User user = userMapper.findById(id);
        if (user == null) throw new UserException(ErrorCodeEnum.USER_NOT_FOUND);

        userMapper.updateTier(id, newTier);

        return userMapper.findById(id);
    }
}

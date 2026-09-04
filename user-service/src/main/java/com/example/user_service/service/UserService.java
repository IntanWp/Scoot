package com.example.user_service.service;

import com.example.user_service.exception.ErrorCodeEnum;
import com.example.user_service.exception.UserException;
import com.example.user_service.mapper.UserMapper;
import com.example.user_service.model.User;
import com.example.user_service.model.dto.request.CreateUserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public void getAllUsers(){
        userMapper.findAll();
    }

    public void createUser(CreateUserRequest request){
        User newUser = User.builder()
                .email(request.getEmail())
                .tier(request.getTier())
                .name(request.getName())
                .createdAt(OffsetDateTime.now())
                .build();

        if (userMapper.emailExists(request.getEmail()) != null) {
            throw new UserException(ErrorCodeEnum.EMAIL_ALREADY_EXISTS);
        }


        userMapper.insert(newUser);
    }

    public User findById(String id){
        UUID uuid = UUID.fromString(id);

        User user = userMapper.findById(uuid);
        if(user == null) {
            throw new UserException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        return user;
    }

    public void updateTier(String id, Integer newTier){
        UUID uuid = UUID.fromString(id);
        User user = userMapper.findById(uuid);
        if(user == null) {
            throw new UserException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        userMapper.updateTier(uuid, newTier);

    }
}

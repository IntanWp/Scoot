package com.example.user_service.controller;

import com.example.user_service.model.User;
import com.example.user_service.model.dto.request.CreateUserRequest;
import com.example.user_service.model.dto.request.UpdateTierRequest;
import com.example.user_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> getAllUsers(){
        return userService.getAllUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createuser(@Valid @RequestBody CreateUserRequest request){
        return userService.createUser(request);
    }

    @GetMapping("/{userId}")
    public User getUserById(@PathVariable String userId){
        return userService.findById(userId);
    }

    @PatchMapping("/{userId}/tier")
    public User updateTier(@PathVariable String userId, @Valid @RequestBody UpdateTierRequest request){
        return userService.updateTier(userId, request.getTier());
    }
}

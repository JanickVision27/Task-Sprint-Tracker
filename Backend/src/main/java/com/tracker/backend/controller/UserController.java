package com.tracker.backend.controller;

import com.tracker.backend.dto.AuthResponse;
import com.tracker.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<AuthResponse.UserInfo>> getAllUsers() {
        List<AuthResponse.UserInfo> users = userRepository.findAll().stream()
                .map(user -> new AuthResponse.UserInfo(user.getId(), user.getName(), user.getEmail(), user.getRole()))
                .toList();
        return ResponseEntity.ok(users);
    }
}

package com.tracker.backend.controller;

import com.tracker.backend.dto.AuthResponse;
import com.tracker.backend.entity.Role;
import com.tracker.backend.entity.User;
import com.tracker.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * * All users, optionally filtered by role.
     *
     * * The "Assign manager" picker in the UI calls this with ?role=MANAGER so it only offers
     * * accounts that are actually valid project managers. Without the parameter the behaviour
     * * is unchanged for existing callers.
     */
    @GetMapping
    public ResponseEntity<List<AuthResponse.UserInfo>> getAllUsers(
            @RequestParam(name = "role", required = false) Role role) {

        List<User> users = (role == null)
                ? userRepository.findAll()
                : userRepository.findByRoleOrderByNameAsc(role);

        List<AuthResponse.UserInfo> responses = users.stream()
                .map(user -> new AuthResponse.UserInfo(user.getId(), user.getName(), user.getEmail(), user.getRole()))
                .toList();

        return ResponseEntity.ok(responses);
    }
}

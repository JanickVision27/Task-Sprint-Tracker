package com.tracker.backend.service;

import com.tracker.backend.dto.AuthResponse;
import com.tracker.backend.dto.RegisterRequest;
import com.tracker.backend.entity.Role;
import com.tracker.backend.entity.User;
import com.tracker.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        //! 1. Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already in use: " + request.getEmail());
        }

        //! 2. Create new User object
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        //! 3. SCRAMBLE THE PASSWORD! Never save plain text.
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        //! 4. Set role. If they didn't provide one, default to MEMBER
        if (request.getRole() == null) {
            user.setRole(Role.MEMBER);
        } else {
            user.setRole(request.getRole());
        }

        //! 5. Save to database
        userRepository.save(user);

        return new AuthResponse("User registered successfully!");
    }
}

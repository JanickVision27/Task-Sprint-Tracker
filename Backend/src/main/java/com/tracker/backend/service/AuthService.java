package com.tracker.backend.service;

import com.tracker.backend.dto.AuthResponse;
import com.tracker.backend.dto.LoginRequest;
import com.tracker.backend.dto.RegisterRequest;
import com.tracker.backend.entity.Role;
import com.tracker.backend.entity.User;
import com.tracker.backend.repository.UserRepository;
import com.tracker.backend.service.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager; // Verifies login credentials
    private final JwtService jwtService;                       // Generates the JWT token

    // Constructor: Spring automatically injects all 4 dependencies here
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        // 1. Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already in use: " + request.getEmail());
        }

        // 2. Create new User object
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // 3. SCRAMBLE THE PASSWORD! Never save plain text.
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 4. Set role. If they didn't provide one, default to MEMBER
        if (request.getRole() == null) {
            user.setRole(Role.MEMBER);
        } else {
            user.setRole(request.getRole());
        }

        // 5. Save to database
        userRepository.save(user);

        return new AuthResponse("User registered successfully!");
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Ask Spring Security to verify the email and password.
        // If the password is wrong, this throws a BadCredentialsException automatically.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. If we get here, the password was correct! Find the user in our DB.
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication."));

        // 3. Generate the JWT "keycard" for this user
        String jwtToken = jwtService.generateToken(user.getEmail());

        // 4. Return the token to the frontend
        return new AuthResponse("Login successful!", jwtToken);
    }
}

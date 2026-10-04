package com.tracker.backend.service;

import com.tracker.backend.dto.AuthResponse;
import com.tracker.backend.dto.LoginRequest;
import com.tracker.backend.dto.RegisterRequest;
import com.tracker.backend.entity.Role;
import com.tracker.backend.entity.User;
import com.tracker.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// * Unit tests for AuthService and JwtService covering registration, login, BCrypt hashing, and JWT tokens.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = new User(1L, "Alice", "alice@test.com", "hashed_pw", Role.MEMBER);
    }

    // Test 1: Registering a new user hashes the password and defaults role to MEMBER
    @Test
    void register_Success_DefaultsToMemberRole() {
        // 1. ARRANGE
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice");
        request.setEmail("alice@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed_pw");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // 2. ACT
        AuthResponse response = authService.register(request);

        // 3. ASSERT
        assertEquals("User registered successfully!", response.getMessage());
        assertNotNull(response.getUser());
        assertEquals(Role.MEMBER, response.getUser().getRole());
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    // Test 2: Registering with an existing email throws IllegalStateException
    @Test
    void register_DuplicateEmail_ThrowsException() {
        // 1. ARRANGE
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice");
        request.setEmail("alice@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(savedUser));

        // 2. ACT & 3. ASSERT
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> authService.register(request)
        );
        assertTrue(ex.getMessage().contains("Email already in use"));
        verify(userRepository, never()).save(any(User.class));
    }

    // Test 3: Logging in with valid credentials returns JWT token and user details
    @Test
    void login_Success_ReturnsJwtTokenAndUserInfo() {
        // 1. ARRANGE
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(savedUser));
        when(jwtService.generateToken("alice@test.com")).thenReturn("mock.jwt.token");

        // 2. ACT
        AuthResponse response = authService.login(request);

        // 3. ASSERT
        assertEquals("Login successful!", response.getMessage());
        assertEquals("mock.jwt.token", response.getToken());
        assertNotNull(response.getUser());
        assertEquals("alice@test.com", response.getUser().getEmail());
    }

    // Test 4: Logging in with wrong password throws BadCredentialsException
    @Test
    void login_BadCredentials_ThrowsException() {
        // 1. ARRANGE
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@test.com");
        request.setPassword("wrong_pw");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // 2. ACT & 3. ASSERT
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(anyString());
    }

    // Test 5: JwtService generates a valid HMAC-SHA256 JWT token and extracts the subject email
    @Test
    void jwtService_GenerateAndValidateToken() {
        // 1. ARRANGE
        JwtService realJwtService = new JwtService();
        ReflectionTestUtils.setField(realJwtService, "jwtSecret", "my-super-secret-test-key-that-is-over-32-bytes-long");
        ReflectionTestUtils.setField(realJwtService, "jwtExpiration", 3600000L);

        // 2. ACT
        String token = realJwtService.generateToken("alice@test.com");

        // 3. ASSERT
        assertNotNull(token);
        assertTrue(realJwtService.isTokenValid(token));
        assertEquals("alice@test.com", realJwtService.extractEmail(token));
        assertFalse(realJwtService.isTokenValid(token + "tampered"));
    }
}

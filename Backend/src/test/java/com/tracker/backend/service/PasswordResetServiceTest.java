package com.tracker.backend.service;

import com.tracker.backend.entity.PasswordResetToken;
import com.tracker.backend.entity.Role;
import com.tracker.backend.entity.User;
import com.tracker.backend.repository.PasswordResetTokenRepository;
import com.tracker.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// * Unit tests for the "forgot password" flow: token creation, single-use tokens,
// * expiry handling and the no-user-enumeration guarantee.
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private PasswordResetService passwordResetService;

    private User user;

    private static final long TTL_MINUTES = 30L;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                emailService,
                TTL_MINUTES);
        user = new User(7L, "Alice", "alice@test.com", "hashed_old_pw", Role.MEMBER);
    }

    // * A request for an address that is not registered must do nothing and reveal nothing.
    @Test
    void requestReset_UnknownEmail_SendsNothing() {
        when(userRepository.findByEmailIgnoreCase("ghost@test.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("ghost@test.com");

        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyLong());
    }

    // * A known address gets a stored SHA-256 hash (never the raw token) plus one email.
    @Test
    void requestReset_KnownEmail_StoresHashAndSendsEmail() {
        when(userRepository.findByEmailIgnoreCase("Alice@Test.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.requestReset("  Alice@Test.com  ");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken stored = tokenCaptor.getValue();

        assertSame(user, stored.getUser());
        assertEquals(64, stored.getTokenHash().length(), "SHA-256 hex digest is 64 characters");
        assertFalse(stored.isUsed());
        assertTrue(stored.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(TTL_MINUTES - 1)));
        verify(passwordResetTokenRepository).deleteByUser(user);

        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(
                eq("Alice"), eq("alice@test.com"), rawTokenCaptor.capture(), eq(TTL_MINUTES));

        String rawToken = rawTokenCaptor.getValue();
        assertNotNull(rawToken);
        assertTrue(rawToken.length() >= 40, "32 random bytes encode to a 43-character token");
        assertEquals(stored.getTokenHash(), PasswordResetService.hashToken(rawToken),
                "the stored hash must match the token that was emailed");
        assertNotEquals(rawToken, stored.getTokenHash(), "the raw token must never be stored");
    }

    @Test
    void validateToken_UsableToken_ReturnsValidWithMaskedEmail() {
        PasswordResetToken token = usableToken();
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        PasswordResetService.PasswordResetTokenValidation result = passwordResetService.validateToken("raw-token");

        assertTrue(result.valid());
        assertEquals("al***@test.com", result.maskedEmail());
    }

    @Test
    void validateToken_UnknownToken_ReturnsInvalid() {
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        PasswordResetService.PasswordResetTokenValidation result = passwordResetService.validateToken("forged");

        assertFalse(result.valid());
        assertEquals(PasswordResetService.INVALID_TOKEN_MESSAGE, result.message());
        assertNull(result.maskedEmail());
    }

    @Test
    void validateToken_ExpiredToken_ReturnsExpiredMessage() {
        PasswordResetToken token = usableToken();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        PasswordResetService.PasswordResetTokenValidation result = passwordResetService.validateToken("raw-token");

        assertFalse(result.valid());
        assertEquals(PasswordResetService.EXPIRED_TOKEN_MESSAGE, result.message());
    }

    @Test
    void resetPassword_ValidToken_HashesNewPasswordAndConsumesToken() {
        PasswordResetToken token = usableToken();
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("brand-new-password")).thenReturn("hashed_new_pw");
        when(passwordResetTokenRepository.findAllByUser(user)).thenReturn(List.of(token));

        passwordResetService.resetPassword("raw-token", "brand-new-password");

        assertEquals("hashed_new_pw", user.getPassword());
        verify(userRepository).save(user);
        assertTrue(token.isUsed());
        assertNotNull(token.getUsedAt());
        verify(passwordResetTokenRepository, atLeastOnce()).save(token);
    }

    @Test
    void resetPassword_ExpiredToken_ThrowsAndLeavesPasswordUntouched() {
        PasswordResetToken token = usableToken();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> passwordResetService.resetPassword("raw-token", "brand-new-password"));

        assertEquals(PasswordResetService.EXPIRED_TOKEN_MESSAGE, ex.getMessage());
        assertEquals("hashed_old_pw", user.getPassword());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void resetPassword_ShortPassword_Throws() {
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(usableToken()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> passwordResetService.resetPassword("raw-token", "short"));

        assertTrue(ex.getMessage().contains("at least 8"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void hashToken_IsDeterministicAndUrlSafe() {
        String first = PasswordResetService.hashToken("abc");
        String second = PasswordResetService.hashToken("abc");

        assertEquals(first, second);
        assertEquals(64, first.length());
        assertTrue(first.matches("[0-9a-f]{64}"));
        assertNotEquals(first, PasswordResetService.hashToken("abd"));
    }

    private PasswordResetToken usableToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(99L);
        token.setUser(user);
        token.setTokenHash(PasswordResetService.hashToken("raw-token"));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(TTL_MINUTES));
        token.setUsed(false);
        return token;
    }
}

package com.tracker.backend.service;

import com.tracker.backend.entity.PasswordResetToken;
import com.tracker.backend.entity.User;
import com.tracker.backend.repository.PasswordResetTokenRepository;
import com.tracker.backend.repository.UserRepository;
import com.tracker.backend.service.mail.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * * "Forgot password" lifecycle:
 * *   1. requestReset(email)  -> creates a single-use token and emails the link through Brevo
 * *   2. validateToken(token) -> lets the UI check the link before showing the form
 * *   3. resetPassword(token, newPassword) -> BCrypt-hashes and stores the new password
 *
 * * Two deliberate security choices:
 * *   - requestReset never reveals whether an account exists (no user enumeration).
 * *   - Only the SHA-256 hash of the token is stored, so the raw token lives only in
 * *     the recipient's inbox.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    /** One message for "never existed", "already used" and "forged" links. */
    static final String INVALID_TOKEN_MESSAGE =
            "This password reset link is invalid or has already been used. Please request a new one.";
    static final String EXPIRED_TOKEN_MESSAGE =
            "This password reset link has expired. Please request a new one.";
    static final String PASSWORD_TOO_SHORT_MESSAGE =
            "Password must be at least %d characters long.";

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long tokenTtlMinutes;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService,
                                @Value("${app.password-reset.token-ttl-minutes:30}") long tokenTtlMinutes) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    /**
     * * Step 1 — start the reset.
     *
     * * Always completes quietly: the controller returns the same generic message whether or
     * * not the address belongs to an account. That prevents attackers from using this
     * * endpoint to discover which emails are registered.
     */
    @Transactional
    public void requestReset(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return;
        }
        String email = rawEmail.trim();

        // * Cheap housekeeping: rows older than a day can never be used again.
        passwordResetTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now().minusDays(1));

        Optional<User> maybeUser = userRepository.findByEmailIgnoreCase(email);
        if (maybeUser.isEmpty()) {
            log.info("Password reset requested for an unknown address {} — nothing sent.", EmailSender.mask(email));
            return;
        }

        User user = maybeUser.get();

        // * Only the newest link should work, so older ones are revoked first.
        passwordResetTokenRepository.deleteByUser(user);

        String rawToken = generateRawToken();

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(tokenTtlMinutes));
        token.setUsed(false);
        passwordResetTokenRepository.save(token);

        // * @Async inside EmailService: the HTTP response does not wait for Brevo.
        emailService.sendPasswordResetEmail(user.getName(), user.getEmail(), rawToken, tokenTtlMinutes);
    }

    /** * Step 2 — used by the reset page to decide between the form and an "expired link" state. */
    @Transactional(readOnly = true)
    public PasswordResetTokenValidation validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return new PasswordResetTokenValidation(false, INVALID_TOKEN_MESSAGE, null);
        }

        Optional<PasswordResetToken> maybeToken =
                passwordResetTokenRepository.findByTokenHash(hashToken(rawToken.trim()));

        if (maybeToken.isEmpty()) {
            return new PasswordResetTokenValidation(false, INVALID_TOKEN_MESSAGE, null);
        }

        PasswordResetToken token = maybeToken.get();
        if (token.isUsed()) {
            return new PasswordResetTokenValidation(false, INVALID_TOKEN_MESSAGE, null);
        }
        if (token.isExpired()) {
            return new PasswordResetTokenValidation(false, EXPIRED_TOKEN_MESSAGE, null);
        }

        String maskedEmail = EmailSender.mask(token.getUser().getEmail());
        return new PasswordResetTokenValidation(true, "Link verified. Choose a new password.", maskedEmail);
    }

    /**
     * * Step 3 — consume the token and store the new BCrypt hash.
     *
     * * Throws IllegalArgumentException for bad input so GlobalExceptionHandler turns it into
     * * a clean 400 with the message shown in the UI.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetTokenValidation validation = validateToken(rawToken);
        if (!validation.valid()) {
            throw new IllegalArgumentException(validation.message());
        }

        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    String.format(PASSWORD_TOO_SHORT_MESSAGE, MIN_PASSWORD_LENGTH));
        }
        if (newPassword.length() > 72) {
            throw new IllegalArgumentException("Password must be at most 72 characters long.");
        }

        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHash(hashToken(rawToken.trim()))
                .orElseThrow(() -> new IllegalArgumentException(INVALID_TOKEN_MESSAGE));

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);

        // * Any other outstanding link for this account stops working immediately.
        passwordResetTokenRepository.findAllByUser(user).stream()
                .filter(other -> !other.getId().equals(token.getId()))
                .forEach(passwordResetTokenRepository::delete);

        log.info("Password successfully reset for {}", EmailSender.mask(user.getEmail()));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** 256 bits of entropy, URL-safe so it survives email clients and query strings. */
    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hex digest — fast and sufficient because the token is random, not a password. */
    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            // * SHA-256 is mandatory in every JVM; this can only happen on a broken runtime.
            throw new IllegalStateException("SHA-256 is not available in this JVM", ex);
        }
    }

    /**
     * * Small value object returned by {@link #validateToken(String)}.
     */
    public record PasswordResetTokenValidation(boolean valid, String message, String maskedEmail) {
    }
}

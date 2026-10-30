package com.tracker.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * * A single-use password reset link.
 *
 * * Security notes
 * *  - Only the SHA-256 hash of the emailed token is stored, so a database leak cannot be
 * *    replayed to take over accounts (same reasoning as never storing plain passwords).
 * *  - Tokens expire (default 30 minutes) and are deleted as soon as they are used or
 * *    replaced by a newer request.
 */
@Entity
@Table(
        name = "password_reset_tokens",
        indexes = {
                @Index(name = "idx_password_reset_token_hash", columnList = "token_hash", unique = true),
                @Index(name = "idx_password_reset_user", columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHA-256 hex digest of the raw token that was emailed to the user. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isExpired() {
        return expiresAt == null || expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isUsable() {
        return !used && !isExpired();
    }
}

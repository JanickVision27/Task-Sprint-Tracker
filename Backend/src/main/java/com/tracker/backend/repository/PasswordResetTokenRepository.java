package com.tracker.backend.repository;

import com.tracker.backend.entity.PasswordResetToken;
import com.tracker.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /** Look up an emailed token by the hash we stored. */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Every outstanding link for one account, used to revoke older ones. */
    List<PasswordResetToken> findAllByUser(User user);

    /** Drop all links for an account (called when a new reset is requested). */
    void deleteByUser(User user);

    /** Housekeeping: remove rows that can no longer be used. */
    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}

package com.tracker.backend.repository;

import com.tracker.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // We will need this later for Login! 
    //* Spring generates: SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    //* Emails are case-insensitive to humans, so "Alice@Test.com" must find "alice@test.com".
    //* Used by the forgot-password flow instead of findByEmail so users can type their address
    //* however their keyboard autocapitalised it.
    Optional<User> findByEmailIgnoreCase(String email);
}

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
}

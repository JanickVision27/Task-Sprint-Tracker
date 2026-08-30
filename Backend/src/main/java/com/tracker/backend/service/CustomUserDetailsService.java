package com.tracker.backend.service;

import com.tracker.backend.entity.User;                                      // Our database User entity
import com.tracker.backend.repository.UserRepository;                        // Our database interface
import org.springframework.security.core.GrantedAuthority;                   // Represents a user's permission/role
import org.springframework.security.core.authority.SimpleGrantedAuthority;   // Basic implementation of GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails;            // Spring Security's internal user shape
import org.springframework.security.core.userdetails.UserDetailsService;     // Spring Security's interface for loading users
import org.springframework.security.core.userdetails.UsernameNotFoundException; 
import org.springframework.stereotype.Service;                               // Registers this as a Spring component

import java.util.Collection; 
import java.util.List;     

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //! Spring Security calls this method automatically when it needs to verify a user
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        
        // 1. Find user in our database by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // 2. Convert our custom Role enum into Spring Security's GrantedAuthority format
        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        // 3. Return a Spring Security UserDetails object (this is the format Spring understands)
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}
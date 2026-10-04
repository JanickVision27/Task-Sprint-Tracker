package com.tracker.backend.dto;

import com.tracker.backend.entity.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

    private String message;
    private String token;
    private UserInfo user;

    //! Constructor for Registration (no token yet)
    public AuthResponse(String message) {
        this.message = message;
    }

    //! Constructor for Login (includes the token)
    public AuthResponse(String message, String token) {
        this.message = message;
        this.token = token;
    }

    //! Constructor for Login with user profile details
    public AuthResponse(String message, String token, UserInfo user) {
        this.message = message;
        this.token = token;
        this.user = user;
    }

    @Getter
    @Setter
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
        private Role role;

        public UserInfo() {
        }

        public UserInfo(Long id, String name, String email, Role role) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
        }
    }
}

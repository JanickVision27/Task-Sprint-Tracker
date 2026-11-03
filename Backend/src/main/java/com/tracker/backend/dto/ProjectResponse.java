package com.tracker.backend.dto;

import com.tracker.backend.entity.Role;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * * The assigned manager, flattened onto the project instead of nesting the whole user.
     *
     * * Flattening keeps the JSON easy for the React UI (no nested null checks) and avoids
     * * ever leaking a password hash through a serialized entity. All three fields are null
     * * when the project has no manager yet.
     */
    private Long managerId;
    private String managerName;
    private String managerEmail;
    private Role managerRole;
}

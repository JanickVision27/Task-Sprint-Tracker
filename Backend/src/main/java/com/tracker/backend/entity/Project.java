package com.tracker.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity // This refers to Database Table not a Normal Class
@Table(name = "projects") // Name of the table project
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// Using Lombok Feature I able to like work on the, create a behind
// getter and setter methods, no need to write them manually


public class Project {

    @Id // Assigning the ID for each value that is generate
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * * The Manager responsible for this project, chosen by the Admin who created it.
     *
     * * It is optional: projects created before this feature (and projects awaiting a new
     * * manager) simply have a NULL manager_id, so the column must stay nullable and the
     * * schema can be upgraded in place by Hibernate's ddl-auto=update.
     *
     * * LAZY keeps the project list cheap — ProjectRepository fetches the manager in the same
     * * query (see @EntityGraph there) only where the DTO needs it.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sprint> sprints = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}





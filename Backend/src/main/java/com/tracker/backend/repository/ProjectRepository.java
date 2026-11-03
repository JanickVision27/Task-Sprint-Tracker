package com.tracker.backend.repository;

import com.tracker.backend.entity.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * * The assigned manager is fetched in the same query as the project.
     *
     * * This matters twice over:
     * *  - spring.jpa.open-in-view=false, so a LAZY manager would already be detached when
     * *    ProjectService copies it into the response DTO (LazyInitializationException).
     * *  - without the join, listing 10 projects would run 10 extra SELECTs for their managers
     * *    (the classic N+1 query problem).
     */
    @Override
    @EntityGraph(attributePaths = "manager")
    List<Project> findAll();

    @Override
    @EntityGraph(attributePaths = "manager")
    Optional<Project> findById(Long id);
}

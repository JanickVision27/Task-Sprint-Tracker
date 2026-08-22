package com.tracker.backend.repository;

import com.tracker.backend.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    
    // * Derived Query: Spring reads the method name and auto-generates the SQL.
    // * "findBy" -> SELECT ... "ProjectId" -> WHERE project_id = ?
    // ! Returns a List because one Project can have many Sprints.
    List<Sprint> findByProjectId(Long projectId);
}
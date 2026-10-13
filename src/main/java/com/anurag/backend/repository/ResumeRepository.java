package com.anurag.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.anurag.backend.model.Resume;
import com.anurag.backend.model.User;

/**
 * Repository for Resume entity.
 * Spring Data JPA automatically generates SQL based on method names.
 */
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    // Get all resumes belonging to a specific user
    List<Resume> findByUser(User user);

    // Get a specific resume that belongs to a specific user (for ownership check)
    Optional<Resume> findByIdAndUser(Long id, User user);
}

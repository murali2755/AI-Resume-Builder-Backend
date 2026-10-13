package com.anurag.backend.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * User entity mapped to the 'users' table in MySQL.
 *
 * Schema (preserved exactly as-is):
 *   id          INT AUTO_INCREMENT PRIMARY KEY
 *   name        VARCHAR(100) NOT NULL
 *   email       VARCHAR(100) NOT NULL UNIQUE
 *   password    VARCHAR(255) NOT NULL
 *   created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 *
 * NOTE: 'id' uses Integer (INT) to match the existing INT column in MySQL.
 */
@Entity
@Table(name = "users")
public class User {

    // Use Integer to match existing MySQL INT column (not BIGINT)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Integer id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // Password is stored as BCrypt hash. Never returned in API responses.
    @NotBlank(message = "Password is required")
    @Column(nullable = false, length = 255)
    @JsonIgnore
    private String password;

    @Column(name = "created_at", insertable = true, updatable = false)
    private LocalDateTime createdAt;

    // One user can have many resumes
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Resume> resumes;

    // Automatically set createdAt before persisting a new user
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Default constructor (required by JPA)
    public User() {
    }

    // ---- Getters and Setters ----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Resume> getResumes() {
        return resumes;
    }

    public void setResumes(List<Resume> resumes) {
        this.resumes = resumes;
    }
}
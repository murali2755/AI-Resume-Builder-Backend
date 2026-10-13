package com.anurag.backend.dto;

/**
 * DTO returned in API responses when user info is needed.
 * Does NOT contain password.
 */
public class UserResponse {

    private Integer id;
    private String name;
    private String email;

    // Default constructor
    public UserResponse() {
    }

    public UserResponse(Integer id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    // Getters and Setters

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
}

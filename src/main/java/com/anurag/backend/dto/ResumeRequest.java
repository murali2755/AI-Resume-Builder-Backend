package com.anurag.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating or updating a resume.
 * All fields are optional — the user can fill only what they want.
 *
 * Constraints match the entity column definitions:
 *   college VARCHAR(200), degree VARCHAR(100), branch VARCHAR(100),
 *   summary/skills/projects/experience TEXT (capped at 10000 chars to guard against abuse).
 *   cgpa DOUBLE in [0, 10].
 *   graduationYear INT in [1950, 2100].
 */
public class ResumeRequest {

    @Size(max = 10000, message = "Summary must not exceed 10000 characters")
    private String summary;

    @Size(max = 200, message = "College must not exceed 200 characters")
    private String college;

    @Size(max = 100, message = "Degree must not exceed 100 characters")
    private String degree;

    @Size(max = 100, message = "Branch must not exceed 100 characters")
    private String branch;

    @DecimalMin(value = "0.0", message = "CGPA must be at least 0.0")
    @DecimalMax(value = "10.0", message = "CGPA must not exceed 10.0")
    private Double cgpa;

    @Min(value = 1950, message = "Graduation year must be 1950 or later")
    @Max(value = 2100, message = "Graduation year must be 2100 or earlier")
    private Integer graduationYear;

    @Size(max = 10000, message = "Skills must not exceed 10000 characters")
    private String skills;

    @Size(max = 10000, message = "Projects must not exceed 10000 characters")
    private String projects;

    @Size(max = 10000, message = "Experience must not exceed 10000 characters")
    private String experience;

    // Default constructor
    public ResumeRequest() {
    }

    // Getters and Setters

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Double getCgpa() {
        return cgpa;
    }

    public void setCgpa(Double cgpa) {
        this.cgpa = cgpa;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getProjects() {
        return projects;
    }

    public void setProjects(String projects) {
        this.projects = projects;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }
}

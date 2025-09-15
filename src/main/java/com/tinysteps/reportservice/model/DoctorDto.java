package com.tinysteps.reportservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for doctor information from doctor service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDto {
    private String id;
    private String userId;
    private String name;  // Added to match API response
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String specialization;
    private String licenseNumber;
    private Integer experienceYears;
    private String qualification;
    private String department;
    private String status;
    private List<String> availableDays;
    private String startTime;
    private String endTime;
    private Integer consultationDuration;
    
    /**
     * Gets the full name of the doctor
     */
    public String getFullName() {
        // First try the 'name' field which is what the API returns
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        // Fallback to firstName + lastName
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return email; // Fallback to email if no name available
    }
}
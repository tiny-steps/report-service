package com.tinysteps.reportservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user information from user service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String name;  // Changed from firstName/lastName to match API response
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role;
    private String domainType;
    private String status;
    private String primaryContextId;
    private String primaryBranchId;
    
    /**
     * Gets the full name of the user
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
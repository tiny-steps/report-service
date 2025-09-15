package com.tinysteps.reportservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response wrapper for patient service API calls
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientServiceResponse {
    private String status;
    private String message;
    private PatientDto data;
    private String instant;
}
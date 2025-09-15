package com.tinysteps.reportservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper class for the response from the schedule service.
 * This handles the case when the response is an object containing
 * a list of appointments rather than a direct array of appointments.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {
    private List<AppointmentDto> appointments;

    // You can add more fields if the response contains additional metadata
}

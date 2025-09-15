package com.tinysteps.reportservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionTypeDto {
    private String id;
    private String name;
    private String description;
    private Integer defaultDurationMinutes;
    private boolean telemedicineAvailable;
    private boolean active;
}
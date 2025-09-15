package com.tinysteps.reportservice.model;

import lombok.Data;

@Data
public class DoctorServiceResponse {
    private DoctorDto data;
    private String message;
    private boolean success;
}
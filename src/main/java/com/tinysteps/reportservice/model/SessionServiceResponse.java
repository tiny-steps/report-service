package com.tinysteps.reportservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class SessionServiceResponse {
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionTypeResponse {
        private SessionTypeDto data;
        private String message;
        private boolean success;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionOfferingResponse {
        private SessionOfferingDto data;
        private String message;
        private boolean success;
    }
}
package com.tinysteps.reportservice.model;

import lombok.Data;

@Data
public class UserServiceResponse {
    private UserDto data;
    private String message;
    private boolean success;
}
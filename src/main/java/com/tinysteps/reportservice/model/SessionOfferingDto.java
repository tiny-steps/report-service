package com.tinysteps.reportservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionOfferingDto {
    private String id;
    private String doctorId;
    private String branchId;
    private SessionTypeDto sessionType;
    private BigDecimal price;
    private boolean active;
}
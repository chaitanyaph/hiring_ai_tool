package com.cadence.jobservice.client.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDto {
    private UUID id;
    private UUID companyId;
    private String departmentName;
}

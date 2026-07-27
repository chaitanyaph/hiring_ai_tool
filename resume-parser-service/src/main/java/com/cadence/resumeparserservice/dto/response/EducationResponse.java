package com.cadence.resumeparserservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EducationResponse {
    private String institutionName;
    private String degree;
    private String fieldOfStudy;
    private String startDate;
    private String endDate;
    private String grade;
}

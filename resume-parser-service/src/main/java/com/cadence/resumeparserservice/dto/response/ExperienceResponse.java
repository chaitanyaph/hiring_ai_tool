package com.cadence.resumeparserservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperienceResponse {
    private String companyName;
    private String designation;
    private String startDate;
    private String endDate;
    private boolean current;
    private String description;
}

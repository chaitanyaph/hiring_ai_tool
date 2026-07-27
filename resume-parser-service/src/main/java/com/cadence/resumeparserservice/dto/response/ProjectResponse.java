package com.cadence.resumeparserservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    private String projectName;
    private String description;
    private String technologies;
}

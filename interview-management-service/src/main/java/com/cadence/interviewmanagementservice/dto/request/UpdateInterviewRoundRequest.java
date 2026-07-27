package com.cadence.interviewmanagementservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInterviewRoundRequest {

    @NotBlank
    private String name;

    private String description;

    private boolean active;

    private Integer roundOrder;
}

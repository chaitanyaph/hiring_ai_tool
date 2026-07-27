package com.cadence.interviewmanagementservice.dto.request;

import com.cadence.interviewmanagementservice.constants.RoundType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInterviewRoundRequest {

    @NotBlank
    private String name;

    @NotNull
    private RoundType type;

    private String description;
}

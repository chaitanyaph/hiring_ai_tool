package com.cadence.codingassessmentservice.dto.request;

import com.cadence.codingassessmentservice.constants.TestCaseVisibility;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTestCaseRequest {
    @NotNull
    private TestCaseVisibility visibility;

    private String inputData;

    @NotBlank
    private String expectedOutput;

    private String explanation;

    @Min(1)
    private Integer weight;
}

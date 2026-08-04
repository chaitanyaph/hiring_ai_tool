package com.cadence.codingassessmentservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkImportTestCasesRequest {
    @NotEmpty @Valid
    private List<CreateTestCaseRequest> testCases;
}

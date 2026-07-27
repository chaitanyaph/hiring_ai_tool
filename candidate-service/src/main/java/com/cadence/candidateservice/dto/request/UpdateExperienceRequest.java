package com.cadence.candidateservice.dto.request;

import jakarta.validation.Valid;
import lombok.*;

import java.util.List;

/** Wizard Step 4 -- full replace, supports add/edit/delete/reorder in one call. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateExperienceRequest {
    @Valid
    private List<ExperienceItemRequest> items;
}

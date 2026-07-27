package com.cadence.candidateservice.dto.request;

import lombok.*;

import java.util.List;

/** Wizard Step 5 -- full replace of the candidate's skill set. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSkillsRequest {
    private List<String> skills;
}

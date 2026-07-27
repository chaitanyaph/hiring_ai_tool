package com.cadence.candidateservice.dto.request;

import lombok.*;

import java.util.List;

/** Wizard Step 8 -- full replace. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLanguagesRequest {
    private List<String> languages;
}

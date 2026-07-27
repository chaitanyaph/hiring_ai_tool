package com.cadence.resumeparserservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LanguageResponse {
    private String languageName;
    private String proficiency;
}

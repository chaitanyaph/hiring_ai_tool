package com.cadence.candidateservice.dto.request;

import lombok.*;

/** Wizard Step 10. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioRequest {
    private String websiteUrl;
    private String linkedinUrl;
    private String githubUrl;
}

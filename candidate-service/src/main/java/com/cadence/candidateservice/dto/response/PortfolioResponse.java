package com.cadence.candidateservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioResponse {
    private String websiteUrl;
    private String linkedinUrl;
    private String githubUrl;
}

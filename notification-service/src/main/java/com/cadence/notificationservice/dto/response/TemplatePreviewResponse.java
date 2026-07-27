package com.cadence.notificationservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplatePreviewResponse {
    private String subject;
    private String bodyHtml;
    private String sampleRecipient;
}

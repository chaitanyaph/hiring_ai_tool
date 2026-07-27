package com.cadence.notificationservice.dto.response;

import com.cadence.notificationservice.constants.PreferenceCategory;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenceResponse {
    private PreferenceCategory category;
    private boolean enabled;
}

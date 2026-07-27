package com.cadence.notificationservice.dto.request;

import com.cadence.notificationservice.constants.PreferenceCategory;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/** Matches the candidate Settings -> Notifications tab: sets all 4 category toggles in one call. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePreferencesRequest {

    @NotEmpty
    private List<PreferenceItem> preferences;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PreferenceItem {
        private PreferenceCategory category;
        private boolean enabled;
    }
}

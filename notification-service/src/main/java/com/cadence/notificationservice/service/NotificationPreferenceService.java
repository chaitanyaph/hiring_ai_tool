package com.cadence.notificationservice.service;

import com.cadence.notificationservice.dto.request.UpdatePreferencesRequest;
import com.cadence.notificationservice.dto.response.PreferenceResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationPreferenceService {

    /** Returns all 4 categories, defaulting unset ones per the Figma (first 3 ON, marketing OFF) without requiring a row to exist yet. */
    List<PreferenceResponse> getPreferences(UUID userId);

    List<PreferenceResponse> updatePreferences(UUID userId, UpdatePreferencesRequest request);
}

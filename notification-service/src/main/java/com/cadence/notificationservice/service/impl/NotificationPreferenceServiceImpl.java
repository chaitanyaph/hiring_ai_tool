package com.cadence.notificationservice.service.impl;

import com.cadence.notificationservice.constants.PreferenceCategory;
import com.cadence.notificationservice.dto.request.UpdatePreferencesRequest;
import com.cadence.notificationservice.dto.response.PreferenceResponse;
import com.cadence.notificationservice.entity.NotificationPreference;
import com.cadence.notificationservice.repository.NotificationPreferenceRepository;
import com.cadence.notificationservice.service.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    /** Matches the Figma's candidate Settings -> Notifications tab default states exactly. */
    private static final Map<PreferenceCategory, Boolean> DEFAULTS = Map.of(
            PreferenceCategory.APPLICATION_STATUS_UPDATES, true,
            PreferenceCategory.INTERVIEW_REMINDERS, true,
            PreferenceCategory.RECOMMENDED_JOBS, true,
            PreferenceCategory.MARKETING_EMAILS, false
    );

    private final NotificationPreferenceRepository preferenceRepository;

    @Override
    public List<PreferenceResponse> getPreferences(UUID userId) {
        Map<PreferenceCategory, NotificationPreference> existing = preferenceRepository.findAllByUserId(userId).stream()
                .collect(java.util.stream.Collectors.toMap(NotificationPreference::getCategory, p -> p));

        return EnumSet.allOf(PreferenceCategory.class).stream()
                .map(category -> PreferenceResponse.builder()
                        .category(category)
                        .enabled(existing.containsKey(category) ? existing.get(category).isEnabled() : DEFAULTS.get(category))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public List<PreferenceResponse> updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        for (UpdatePreferencesRequest.PreferenceItem item : request.getPreferences()) {
            NotificationPreference preference = preferenceRepository.findByUserIdAndCategory(userId, item.getCategory())
                    .orElseGet(() -> NotificationPreference.builder().userId(userId).category(item.getCategory()).build());
            preference.setEnabled(item.isEnabled());
            preferenceRepository.save(preference);
        }
        return getPreferences(userId);
    }
}

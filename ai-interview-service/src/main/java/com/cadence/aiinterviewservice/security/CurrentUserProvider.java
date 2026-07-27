package com.cadence.aiinterviewservice.security;

import com.cadence.aiinterviewservice.exception.AccessDeniedApiException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public CurrentUser getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (!(principal instanceof CurrentUser currentUser)) {
            throw new AccessDeniedApiException("A valid access token is required");
        }
        return currentUser;
    }
}

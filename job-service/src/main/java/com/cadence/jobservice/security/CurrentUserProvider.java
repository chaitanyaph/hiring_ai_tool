package com.cadence.jobservice.security;

import com.cadence.jobservice.exception.AccessDeniedApiException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public CurrentUser getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (principal instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new AccessDeniedApiException("No authenticated user in context");
    }
}

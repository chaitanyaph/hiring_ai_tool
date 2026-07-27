package com.cadence.authservice.dto.response;

import com.cadence.authservice.constant.AuthProvider;
import com.cadence.authservice.constant.UserStatus;
import com.cadence.authservice.constant.UserType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private UserType userType;
    private UserStatus status;
    private boolean emailVerified;
    private boolean mfaEnabled;
    private AuthProvider authProvider;
    private UUID companyId;
    private String companyName;
    private Set<String> roles;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}

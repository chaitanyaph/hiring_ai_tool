package com.cadence.authservice.dto.response;

import lombok.*;

/**
 * Returned by /login. When mfaRequired is true, accessToken/refreshToken
 * are intentionally null and the client must call
 * /auth/mfa/verify-login with the mfaSessionToken + OTP to complete
 * authentication -- password alone never issues a usable token when MFA
 * is enabled for the account.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private boolean mfaRequired;
    private String mfaSessionToken;
    private TokenResponse tokens;
    private UserResponse user;
}

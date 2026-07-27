package com.cadence.authservice.security;

import com.cadence.authservice.constant.AuthProvider;
import com.cadence.authservice.constant.RoleName;
import com.cadence.authservice.constant.UserStatus;
import com.cadence.authservice.constant.UserType;
import com.cadence.authservice.dto.response.TokenResponse;
import com.cadence.authservice.entity.Role;
import com.cadence.authservice.entity.User;
import com.cadence.authservice.repository.RoleRepository;
import com.cadence.authservice.repository.UserRepository;
import com.cadence.authservice.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles the callback after Google OAuth2 login succeeds. Rather than
 * redirecting with tokens in a URL fragment (leak-prone via browser
 * history/referrer headers), we issue our own JWT pair and return them
 * as JSON, matching the exact response shape of a normal /login call so
 * the frontend has one integration path for both flows.
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub");

        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Role candidateRole = roleRepository.findByName(RoleName.ROLE_CANDIDATE.name())
                    .orElseThrow(() -> new IllegalStateException("Default role not seeded"));
            Set<Role> roles = new HashSet<>();
            roles.add(candidateRole);

            User newUser = User.builder()
                    .fullName(name != null ? name : email)
                    .email(email)
                    .passwordHash("") // no password for OAuth2-only accounts
                    .userType(UserType.CANDIDATE)
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true) // Google already verified it
                    .authProvider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .roles(roles)
                    .build();
            return userRepository.save(newUser);
        });

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet());
        Set<String> permissionNames = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getName())
                .collect(java.util.stream.Collectors.toSet());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), roleNames, permissionNames, user.getCompanyId());
        String refreshToken = refreshTokenService.issueRefreshToken(user.getId(), false, request.getHeader("User-Agent"), request.getRemoteAddr());

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInSeconds(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .build();

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(tokenResponse));
    }
}

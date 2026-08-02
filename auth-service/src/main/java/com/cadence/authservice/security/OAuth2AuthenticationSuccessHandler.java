package com.cadence.authservice.security;

import com.cadence.authservice.constant.AuthProvider;
import com.cadence.authservice.constant.RoleName;
import com.cadence.authservice.constant.UserStatus;
import com.cadence.authservice.constant.UserType;
import com.cadence.authservice.entity.Role;
import com.cadence.authservice.entity.User;
import com.cadence.authservice.repository.RoleRepository;
import com.cadence.authservice.repository.UserRepository;
import com.cadence.authservice.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles the callback after Google OAuth2 login succeeds. This is a full-page
 * browser redirect from Google, not an XHR the Angular SPA can read the body
 * of -- so rather than writing tokens into the response body (which would just
 * render as raw JSON text in the browser, never reaching the SPA), we hand the
 * finished login off to AuthService for a one-time exchange code and redirect
 * the browser back into the SPA, which redeems that code via a normal XHR.
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthService authService;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

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

        String code = authService.issueOAuthExchangeCode(user);
        String redirectUrl = frontendBaseUrl + "/oauth2/callback?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }
}

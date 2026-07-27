package com.cadence.authservice.controller;

import com.cadence.authservice.config.SecurityConfig;
import com.cadence.authservice.dto.request.LoginRequest;
import com.cadence.authservice.dto.request.RegisterRequest;
import com.cadence.authservice.constant.UserType;
import com.cadence.authservice.dto.response.AuthResponse;
import com.cadence.authservice.dto.response.TokenResponse;
import com.cadence.authservice.dto.response.UserResponse;
import com.cadence.authservice.security.JwtAuthenticationFilter;
import com.cadence.authservice.service.AuthService;
import com.cadence.authservice.service.EmailVerificationService;
import com.cadence.authservice.service.PasswordService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for the web layer only. SecurityConfig is excluded from the
 * slice (and its filters disabled via addFilters = false) so the test
 * focuses purely on request/response mapping, validation, and status
 * codes -- the security chain itself is covered separately
 * (JwtTokenProviderTest, and would extend to a full @SpringBootTest +
 * Testcontainers suite for end-to-end auth flows). Without this
 * exclusion, the slice still builds the JwtAuthenticationFilter bean
 * (auto-included since @WebMvcTest always picks up jakarta.servlet.Filter
 * beans, regardless of who declares them), which drags in
 * JwtTokenProvider/Redis beans that a @WebMvcTest slice doesn't provide.
 */
@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private PasswordService passwordService;
    @MockBean private EmailVerificationService emailVerificationService;

    @Test
    void register_shouldReturn201_onValidRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Doe").email("jane@email.com").password("Str0ng!Pass").userType(UserType.CANDIDATE)
                .build();
        when(authService.register(any())).thenReturn(UserResponse.builder().id(UUID.randomUUID()).email("jane@email.com").build());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("jane@email.com"));
    }

    @Test
    void register_shouldReturn400_onInvalidEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Doe").email("not-an-email").password("Str0ng!Pass").userType(UserType.CANDIDATE)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void register_shouldReturn400_onWeakPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Doe").email("jane@email.com").password("weak").userType(UserType.CANDIDATE)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturn200_withTokens_onSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder().email("jane@email.com").password("Str0ng!Pass").build();
        AuthResponse response = AuthResponse.builder()
                .mfaRequired(false)
                .tokens(TokenResponse.builder().accessToken("access").refreshToken("refresh").expiresInSeconds(900).build())
                .user(UserResponse.builder().email("jane@email.com").build())
                .build();
        when(authService.login(any(), anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mfaRequired").value(false))
                .andExpect(jsonPath("$.data.tokens.accessToken").value("access"));
    }

    @Test
    void login_shouldReturn200_withMfaRequired_whenMfaEnabled() throws Exception {
        LoginRequest request = LoginRequest.builder().email("jane@email.com").password("Str0ng!Pass").build();
        AuthResponse response = AuthResponse.builder().mfaRequired(true).mfaSessionToken("session-token").build();
        when(authService.login(any(), anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mfaRequired").value(true))
                .andExpect(jsonPath("$.data.mfaSessionToken").value("session-token"));
    }

    @Test
    void forgotPassword_shouldAlwaysReturn200_regardlessOfAccountExistence() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@email.com\"}"))
                .andExpect(status().isOk());
    }
}

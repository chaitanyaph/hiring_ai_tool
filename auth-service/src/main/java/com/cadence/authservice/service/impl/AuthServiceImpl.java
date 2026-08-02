package com.cadence.authservice.service.impl;

import com.cadence.authservice.constant.AuditEventType;
import com.cadence.authservice.constant.RoleName;
import com.cadence.authservice.constant.UserStatus;
import com.cadence.authservice.dto.request.LoginRequest;
import com.cadence.authservice.dto.request.MfaVerifyRequest;
import com.cadence.authservice.dto.request.RegisterRequest;
import com.cadence.authservice.dto.response.AuthResponse;
import com.cadence.authservice.dto.response.TokenResponse;
import com.cadence.authservice.dto.response.UserResponse;
import com.cadence.authservice.constant.UserType;
import com.cadence.authservice.entity.Company;
import com.cadence.authservice.entity.RefreshToken;
import com.cadence.authservice.entity.Role;
import com.cadence.authservice.entity.User;
import com.cadence.authservice.exception.*;
import com.cadence.authservice.kafka.event.AccountLockedEvent;
import com.cadence.authservice.kafka.event.UserLoggedInEvent;
import com.cadence.authservice.kafka.producer.AuthEventProducer;
import com.cadence.authservice.mapper.UserMapper;
import com.cadence.authservice.repository.CompanyRepository;
import com.cadence.authservice.repository.RoleRepository;
import com.cadence.authservice.repository.UserRepository;
import com.cadence.authservice.security.JwtTokenProvider;
import com.cadence.authservice.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates the end-to-end authentication lifecycle. Deliberately
 * kept as one cohesive service (rather than scattering login logic
 * across controllers) so the account-lockout / MFA-gate / audit rules
 * are enforced in exactly one place regardless of which endpoint
 * triggers a login.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;
    private final MfaService mfaService;
    private final EmailVerificationService emailVerificationService;
    private final AuditLogService auditLogService;
    private final AuthEventProducer eventProducer;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String OAUTH_EXCHANGE_PREFIX = "auth:oauth-exchange:";
    private static final Duration OAUTH_EXCHANGE_TTL = Duration.ofSeconds(60);

    @Value("${app.security.max-failed-login-attempts}")
    private int maxFailedLoginAttempts;

    @Value("${app.security.account-lock-duration-minutes}")
    private long accountLockDurationMinutes;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        Role defaultRole = resolveDefaultRole(request.getUserType());
        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);

        UUID companyId = request.getCompanyId();
        if (request.getUserType() == UserType.COMPANY_ADMIN) {
            companyId = createCompany(request.getCompanyName()).getId();
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .userType(request.getUserType())
                .status(UserStatus.PENDING_VERIFICATION)
                .companyId(companyId)
                .roles(roles)
                .build();

        user = userRepository.save(user);

        emailVerificationService.sendVerification(user.getId());
        auditLogService.record(user.getId(), AuditEventType.REGISTER, "User registered", null, null);

        log.info("New user registered: {} ({})", user.getEmail(), user.getUserType());
        return enrichWithCompanyName(userMapper.toResponse(user));
    }

    private Company createCompany(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            throw new AuthServiceException(ErrorCode.VALIDATION_FAILED,
                    "Company name is required to register a company workspace", HttpStatus.BAD_REQUEST);
        }
        Company company = Company.builder()
                .name(companyName.trim())
                .slug(generateUniqueSlug(companyName))
                .build();
        return companyRepository.save(company);
    }

    private String generateUniqueSlug(String companyName) {
        String base = companyName.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (base.isEmpty()) {
            base = "company";
        }
        String slug = base;
        int suffix = 2;
        while (companyRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private UserResponse enrichWithCompanyName(UserResponse response) {
        if (response != null && response.getCompanyId() != null) {
            companyRepository.findById(response.getCompanyId()).ifPresent(c -> response.setCompanyName(c.getName()));
        }
        return response;
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        enforceAccountNotLocked(user);

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new AccountDisabledException("This account has been disabled. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user, ipAddress, userAgent);
            throw new InvalidCredentialsException();
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        // Successful password check -- reset the failure counter.
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
        }

        if (user.isMfaEnabled()) {
            userRepository.save(user);
            String mfaSessionToken = mfaService.issueMfaSessionToken(user.getId());
            auditLogService.record(user.getId(), AuditEventType.LOGIN_SUCCESS, "Password verified, awaiting MFA challenge", ipAddress, userAgent);
            return AuthResponse.builder()
                    .mfaRequired(true)
                    .mfaSessionToken(mfaSessionToken)
                    .build();
        }

        return finalizeLogin(user, request.isRememberMe(), ipAddress, userAgent);
    }

    @Override
    @Transactional
    public AuthResponse completeMfaLogin(MfaVerifyRequest request, String ipAddress, String userAgent) {
        UUID userId = mfaService.resolveMfaSessionToken(request.getMfaSessionToken());
        User user = userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);

        enforceAccountNotLocked(user);

        if (!mfaService.verifyCode(userId, request.getCode())) {
            auditLogService.record(userId, AuditEventType.MFA_CHALLENGE_FAILURE, "Invalid MFA code during login", ipAddress, userAgent);
            throw new InvalidMfaCodeException();
        }

        auditLogService.record(userId, AuditEventType.MFA_CHALLENGE_SUCCESS, "MFA challenge passed", ipAddress, userAgent);
        return finalizeLogin(user, false, ipAddress, userAgent);
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(String rawRefreshToken, String ipAddress, String userAgent) {
        RefreshToken existing = refreshTokenService.validateAndGet(rawRefreshToken);
        String newRawToken = refreshTokenService.rotate(existing, userAgent, ipAddress);

        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String accessToken = buildAccessToken(user);
        auditLogService.record(user.getId(), AuditEventType.TOKEN_REFRESH, "Access token refreshed", ipAddress, userAgent);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRawToken)
                .expiresInSeconds(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    @Override
    @Transactional
    public void logout(UUID userId, String rawRefreshToken, boolean allDevices) {
        if (allDevices) {
            refreshTokenService.revokeAllForUser(userId);
            sessionService.endAllSessions(userId);
        } else {
            refreshTokenService.revoke(rawRefreshToken);
        }
        auditLogService.record(userId, AuditEventType.LOGOUT, allDevices ? "Logged out of all devices" : "Logged out", null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return enrichWithCompanyName(userMapper.toResponse(user));
    }

    @Override
    @Transactional
    public String issueOAuthExchangeCode(User user) {
        AuthResponse response = finalizeLogin(user, false, null, null);
        String code = UUID.randomUUID().toString();
        try {
            redisTemplate.opsForValue().set(OAUTH_EXCHANGE_PREFIX + code, objectMapper.writeValueAsString(response), OAUTH_EXCHANGE_TTL);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize OAuth2 login payload", e);
        }
        return code;
    }

    @Override
    public AuthResponse exchangeOAuthCode(String code) {
        String key = OAUTH_EXCHANGE_PREFIX + code;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new InvalidTokenException("This login link has expired or was already used. Please try signing in again.");
        }
        redisTemplate.delete(key);
        try {
            return objectMapper.readValue(json, AuthResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize OAuth2 login payload", e);
        }
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private AuthResponse finalizeLogin(User user, boolean rememberMe, String ipAddress, String userAgent) {
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        String accessToken = buildAccessToken(user);
        String rawRefreshToken = refreshTokenService.issueRefreshToken(user.getId(), rememberMe, userAgent, ipAddress);

        auditLogService.record(user.getId(), AuditEventType.LOGIN_SUCCESS, "User logged in", ipAddress, userAgent);
        eventProducer.publishUserLoggedIn(UserLoggedInEvent.builder()
                .userId(user.getId()).email(user.getEmail()).ipAddress(ipAddress).device(userAgent)
                .occurredAt(LocalDateTime.now()).build());

        TokenResponse tokens = TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresInSeconds(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .build();

        return AuthResponse.builder()
                .mfaRequired(false)
                .tokens(tokens)
                .user(enrichWithCompanyName(userMapper.toResponse(user)))
                .build();
    }

    private String buildAccessToken(User user) {
        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        Set<String> permissionNames = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getName())
                .collect(Collectors.toSet());
        return jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), roleNames, permissionNames, user.getCompanyId());
    }

    private void enforceAccountNotLocked(User user) {
        if (!user.isAccountNonLocked()) {
            throw new AccountLockedException("Account is locked until " + user.getAccountLockedUntil()
                    + " due to multiple failed login attempts. Please try again later or reset your password.");
        }
    }

    private void handleFailedLogin(User user, String ipAddress, String userAgent) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        auditLogService.record(user.getId(), AuditEventType.LOGIN_FAILURE,
                "Failed login attempt #" + attempts, ipAddress, userAgent);

        if (attempts >= maxFailedLoginAttempts) {
            LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(accountLockDurationMinutes);
            user.setAccountLockedUntil(lockedUntil);
            auditLogService.record(user.getId(), AuditEventType.ACCOUNT_LOCKED,
                    "Account locked after " + attempts + " failed attempts", ipAddress, userAgent);
            eventProducer.publishAccountLocked(AccountLockedEvent.builder()
                    .userId(user.getId()).email(user.getEmail())
                    .reason("Exceeded maximum failed login attempts")
                    .lockedUntil(lockedUntil).occurredAt(LocalDateTime.now()).build());
        }
        userRepository.save(user);
    }

    private Role resolveDefaultRole(com.cadence.authservice.constant.UserType userType) {
        RoleName roleName = switch (userType) {
            case ADMIN -> RoleName.ROLE_ADMIN;
            case COMPANY_ADMIN -> RoleName.ROLE_COMPANY_ADMIN;
            // UserType.RECRUITER is a coarse self-registration choice; ROLE_HR_RECRUITER is the
            // sensible default. A company admin can upgrade to a more specific recruiting role
            // (HR_MANAGER/TECHNICAL_RECRUITER/TALENT_ACQUISITION_MANAGER/HIRING_MANAGER) via the
            // existing RoleController assign/revoke endpoints -- no code change needed there.
            case RECRUITER -> RoleName.ROLE_HR_RECRUITER;
            case CANDIDATE -> RoleName.ROLE_CANDIDATE;
        };
        return roleRepository.findByName(roleName.name())
                .orElseThrow(() -> new RoleNotFoundException(roleName.name()));
    }
}

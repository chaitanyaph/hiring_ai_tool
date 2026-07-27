package com.cadence.authservice.service;

import com.cadence.authservice.constant.RoleName;
import com.cadence.authservice.constant.UserStatus;
import com.cadence.authservice.constant.UserType;
import com.cadence.authservice.dto.request.LoginRequest;
import com.cadence.authservice.dto.request.RegisterRequest;
import com.cadence.authservice.dto.response.AuthResponse;
import com.cadence.authservice.dto.response.UserResponse;
import com.cadence.authservice.entity.Company;
import com.cadence.authservice.entity.Role;
import com.cadence.authservice.entity.User;
import com.cadence.authservice.exception.AccountLockedException;
import com.cadence.authservice.exception.AuthServiceException;
import com.cadence.authservice.exception.InvalidCredentialsException;
import com.cadence.authservice.exception.UserAlreadyExistsException;
import com.cadence.authservice.kafka.producer.AuthEventProducer;
import com.cadence.authservice.mapper.UserMapper;
import com.cadence.authservice.repository.CompanyRepository;
import com.cadence.authservice.repository.RoleRepository;
import com.cadence.authservice.repository.UserRepository;
import com.cadence.authservice.security.JwtTokenProvider;
import com.cadence.authservice.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private SessionService sessionService;
    @Mock private MfaService mfaService;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private AuditLogService auditLogService;
    @Mock private AuthEventProducer eventProducer;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeUser;
    private Role candidateRole;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "maxFailedLoginAttempts", 5);
        ReflectionTestUtils.setField(authService, "accountLockDurationMinutes", 30L);

        candidateRole = Role.builder().id(UUID.randomUUID()).name(RoleName.ROLE_CANDIDATE.name())
                .permissions(new HashSet<>()).build();

        activeUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Rahul Mehta")
                .email("rahul@email.com")
                .passwordHash("hashed-pw")
                .userType(UserType.CANDIDATE)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .mfaEnabled(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>(Set.of(candidateRole)))
                .build();
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Doe").email("jane@email.com").password("Str0ng!Pass")
                .userType(UserType.CANDIDATE).build();

        when(userRepository.existsByEmailIgnoreCase("jane@email.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldCreateUser_withDefaultRoleAndSendVerification() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Doe").email("jane@email.com").password("Str0ng!Pass")
                .userType(UserType.CANDIDATE).build();

        when(userRepository.existsByEmailIgnoreCase("jane@email.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CANDIDATE.name())).thenReturn(Optional.of(candidateRole));
        when(passwordEncoder.encode("Str0ng!Pass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(userMapper.toResponse(any(User.class))).thenReturn(UserResponse.builder().email("jane@email.com").build());

        UserResponse response = authService.register(request);

        assertThat(response.getEmail()).isEqualTo("jane@email.com");
        verify(emailVerificationService, times(1)).sendVerification(any(UUID.class));
        verify(auditLogService, times(1)).record(any(), any(), anyString(), any(), any());
    }

    @Test
    void register_shouldThrow_whenCompanyAdminOmitsCompanyName() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Ananya Rao").email("ananya@acmecorp.com").password("Str0ng!Pass")
                .userType(UserType.COMPANY_ADMIN).build();

        when(userRepository.existsByEmailIgnoreCase("ananya@acmecorp.com")).thenReturn(false);
        Role companyAdminRole = Role.builder().id(UUID.randomUUID()).name(RoleName.ROLE_COMPANY_ADMIN.name())
                .permissions(new HashSet<>()).build();
        when(roleRepository.findByName(RoleName.ROLE_COMPANY_ADMIN.name())).thenReturn(Optional.of(companyAdminRole));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthServiceException.class);

        verify(companyRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldCreateCompanyAndLinkUser_whenRegisteringAsCompanyAdmin() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Ananya Rao").email("ananya@acmecorp.com").password("Str0ng!Pass")
                .userType(UserType.COMPANY_ADMIN).companyName("Acme Corp").build();

        Role companyAdminRole = Role.builder().id(UUID.randomUUID()).name(RoleName.ROLE_COMPANY_ADMIN.name())
                .permissions(new HashSet<>()).build();
        UUID companyId = UUID.randomUUID();

        when(userRepository.existsByEmailIgnoreCase("ananya@acmecorp.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_COMPANY_ADMIN.name())).thenReturn(Optional.of(companyAdminRole));
        when(companyRepository.existsBySlug("acme-corp")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> {
            Company c = inv.getArgument(0);
            c.setId(companyId);
            return c;
        });
        when(passwordEncoder.encode("Str0ng!Pass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(UserResponse.builder().email("ananya@acmecorp.com").companyId(companyId).build());
        when(companyRepository.findById(companyId))
                .thenReturn(Optional.of(Company.builder().id(companyId).name("Acme Corp").slug("acme-corp").build()));

        UserResponse response = authService.register(request);

        assertThat(response.getCompanyId()).isEqualTo(companyId);
        assertThat(response.getCompanyName()).isEqualTo("Acme Corp");

        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(companyCaptor.capture());
        assertThat(companyCaptor.getValue().getName()).isEqualTo("Acme Corp");
        assertThat(companyCaptor.getValue().getSlug()).isEqualTo("acme-corp");
    }

    @Test
    void login_shouldThrow_whenAccountIsLocked() {
        activeUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmailIgnoreCase("rahul@email.com")).thenReturn(Optional.of(activeUser));

        LoginRequest request = LoginRequest.builder().email("rahul@email.com").password("whatever").build();

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void login_shouldThrowInvalidCredentials_andIncrementFailedAttempts_onWrongPassword() {
        when(userRepository.findByEmailIgnoreCase("rahul@email.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong-password", "hashed-pw")).thenReturn(false);

        LoginRequest request = LoginRequest.builder().email("rahul@email.com").password("wrong-password").build();

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(activeUser.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository, atLeastOnce()).save(activeUser);
    }

    @Test
    void login_shouldLockAccount_afterMaxFailedAttempts() {
        activeUser.setFailedLoginAttempts(4); // one more failure should trip the lock
        when(userRepository.findByEmailIgnoreCase("rahul@email.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        LoginRequest request = LoginRequest.builder().email("rahul@email.com").password("wrong").build();

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(activeUser.getAccountLockedUntil()).isNotNull();
        verify(eventProducer, times(1)).publishAccountLocked(any());
    }

    @Test
    void login_shouldReturnMfaRequired_whenMfaEnabled() {
        activeUser.setMfaEnabled(true);
        when(userRepository.findByEmailIgnoreCase("rahul@email.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("correct-password", "hashed-pw")).thenReturn(true);
        when(mfaService.issueMfaSessionToken(activeUser.getId())).thenReturn("mfa-session-token");

        LoginRequest request = LoginRequest.builder().email("rahul@email.com").password("correct-password").build();

        AuthResponse response = authService.login(request, "127.0.0.1", "JUnit");

        assertThat(response.isMfaRequired()).isTrue();
        assertThat(response.getMfaSessionToken()).isEqualTo("mfa-session-token");
        assertThat(response.getTokens()).isNull();
        verify(refreshTokenService, never()).issueRefreshToken(any(), anyBoolean(), any(), any());
    }

    @Test
    void login_shouldIssueTokens_onSuccessfulLoginWithoutMfa() {
        when(userRepository.findByEmailIgnoreCase("rahul@email.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("correct-password", "hashed-pw")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any(), any())).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.issueRefreshToken(any(), eq(false), any(), any())).thenReturn("refresh-token");
        when(userMapper.toResponse(activeUser)).thenReturn(UserResponse.builder().email("rahul@email.com").build());

        LoginRequest request = LoginRequest.builder().email("rahul@email.com").password("correct-password").rememberMe(false).build();

        AuthResponse response = authService.login(request, "127.0.0.1", "JUnit");

        assertThat(response.isMfaRequired()).isFalse();
        assertThat(response.getTokens().getAccessToken()).isEqualTo("access-token");
        assertThat(response.getTokens().getRefreshToken()).isEqualTo("refresh-token");
        verify(eventProducer, times(1)).publishUserLoggedIn(any());
    }
}

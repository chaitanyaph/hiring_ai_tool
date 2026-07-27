package com.cadence.authservice.security;

import com.cadence.authservice.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Set<GrantedAuthority> authorities;
    private final UUID companyId;
    private final boolean mfaEnabled;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.enabled = user.getStatus() == com.cadence.authservice.constant.UserStatus.ACTIVE
                || user.getStatus() == com.cadence.authservice.constant.UserStatus.PENDING_VERIFICATION;
        this.accountNonLocked = user.isAccountNonLocked();
        this.companyId = user.getCompanyId();
        this.mfaEnabled = user.isMfaEnabled();

        // Roles become ROLE_ authorities; permissions become plain-name authorities,
        // so @PreAuthorize can check either hasRole('RECRUITER') or hasAuthority('JOB_CREATE').
        this.authorities = Stream.concat(
                user.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName())),
                user.getRoles().stream().flatMap(r -> r.getPermissions().stream())
                        .map(p -> new SimpleGrantedAuthority(p.getName()))
        ).collect(Collectors.toSet());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return accountNonLocked; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}

package com.cadence.authservice.repository;

import com.cadence.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByAuthProviderAndProviderId(com.cadence.authservice.constant.AuthProvider provider, String providerId);

    @Modifying
    @Query("update User u set u.failedLoginAttempts = :attempts where u.id = :userId")
    void updateFailedLoginAttempts(@Param("userId") UUID userId, @Param("attempts") int attempts);
}

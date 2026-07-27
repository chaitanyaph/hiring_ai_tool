package com.cadence.authservice.service.impl;

import com.cadence.authservice.constant.AuditEventType;
import com.cadence.authservice.dto.response.RoleResponse;
import com.cadence.authservice.entity.Role;
import com.cadence.authservice.entity.User;
import com.cadence.authservice.exception.ResourceNotFoundException;
import com.cadence.authservice.exception.RoleNotFoundException;
import com.cadence.authservice.mapper.RoleMapper;
import com.cadence.authservice.repository.RoleRepository;
import com.cadence.authservice.repository.UserRepository;
import com.cadence.authservice.service.AuditLogService;
import com.cadence.authservice.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RoleMapper roleMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream().map(roleMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public void assignRoles(UUID userId, Set<String> roleNames, UUID actorUserId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName).orElseThrow(() -> new RoleNotFoundException(roleName));
            user.getRoles().add(role);
        }
        userRepository.save(user);
        auditLogService.record(actorUserId, AuditEventType.ROLE_ASSIGNED,
                "Assigned roles " + roleNames + " to user " + userId, null, null);
    }

    @Override
    @Transactional
    public void revokeRoles(UUID userId, Set<String> roleNames, UUID actorUserId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.getRoles().removeIf(role -> roleNames.contains(role.getName()));
        userRepository.save(user);
        auditLogService.record(actorUserId, AuditEventType.ROLE_REVOKED,
                "Revoked roles " + roleNames + " from user " + userId, null, null);
    }
}

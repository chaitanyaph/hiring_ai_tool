package com.cadence.authservice.mapper;

import com.cadence.authservice.dto.response.RoleResponse;
import com.cadence.authservice.entity.Permission;
import com.cadence.authservice.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "permissions", expression = "java(mapPermissionNames(role.getPermissions()))")
    RoleResponse toResponse(Role role);

    default Set<String> mapPermissionNames(Set<Permission> permissions) {
        if (permissions == null) return Set.of();
        return permissions.stream().map(Permission::getName).collect(Collectors.toSet());
    }
}

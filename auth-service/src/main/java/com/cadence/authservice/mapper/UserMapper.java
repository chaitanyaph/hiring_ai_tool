package com.cadence.authservice.mapper;

import com.cadence.authservice.dto.response.UserResponse;
import com.cadence.authservice.entity.Role;
import com.cadence.authservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(mapRoleNames(user.getRoles()))")
    UserResponse toResponse(User user);

    default Set<String> mapRoleNames(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream().map(Role::getName).collect(Collectors.toSet());
    }
}

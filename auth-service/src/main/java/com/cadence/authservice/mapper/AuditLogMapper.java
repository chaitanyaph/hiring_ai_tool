package com.cadence.authservice.mapper;

import com.cadence.authservice.dto.response.AuditLogResponse;
import com.cadence.authservice.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {
    AuditLogResponse toResponse(AuditLog auditLog);
}

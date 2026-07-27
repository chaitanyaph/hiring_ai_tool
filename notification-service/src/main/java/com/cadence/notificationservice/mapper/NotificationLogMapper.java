package com.cadence.notificationservice.mapper;

import com.cadence.notificationservice.dto.response.NotificationLogResponse;
import com.cadence.notificationservice.entity.NotificationLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationLogMapper {
    NotificationLogResponse toResponse(NotificationLog log);
}

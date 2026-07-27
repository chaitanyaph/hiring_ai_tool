package com.cadence.notificationservice.mapper;

import com.cadence.notificationservice.dto.response.NotificationResponse;
import com.cadence.notificationservice.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);
}

package com.cadence.notificationservice.mapper;

import com.cadence.notificationservice.dto.response.TemplateResponse;
import com.cadence.notificationservice.entity.NotificationTemplate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TemplateMapper {
    TemplateResponse toResponse(NotificationTemplate template);
}

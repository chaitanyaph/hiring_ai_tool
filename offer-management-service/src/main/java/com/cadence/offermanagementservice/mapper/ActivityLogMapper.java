package com.cadence.offermanagementservice.mapper;

import com.cadence.offermanagementservice.dto.response.ActivityLogResponse;
import com.cadence.offermanagementservice.entity.OfferActivityLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ActivityLogMapper {
    ActivityLogResponse toResponse(OfferActivityLog log);
}

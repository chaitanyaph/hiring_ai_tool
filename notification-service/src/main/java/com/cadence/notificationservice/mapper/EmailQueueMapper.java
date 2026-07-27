package com.cadence.notificationservice.mapper;

import com.cadence.notificationservice.dto.response.EmailQueueDetailResponse;
import com.cadence.notificationservice.dto.response.EmailQueueItemResponse;
import com.cadence.notificationservice.entity.EmailQueue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** templateCategory is enriched by the service layer (a lookup by templateId), not stored redundantly on email_queue. */
@Mapper(componentModel = "spring")
public interface EmailQueueMapper {

    @Mapping(target = "templateCategory", ignore = true)
    EmailQueueItemResponse toItemResponse(EmailQueue emailQueue);

    @Mapping(target = "attachments", ignore = true)
    EmailQueueDetailResponse toDetailResponse(EmailQueue emailQueue);
}

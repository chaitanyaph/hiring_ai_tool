package com.cadence.notificationservice.service;

import com.cadence.notificationservice.dto.response.NotificationLogResponse;
import com.cadence.notificationservice.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface NotificationLogService {

    /** Backs notif-logs. source filter is optional (matches the Figma's "All services / Email dispatcher / ..." dropdown). */
    PagedResponse<NotificationLogResponse> listLogs(String source, Pageable pageable);
}

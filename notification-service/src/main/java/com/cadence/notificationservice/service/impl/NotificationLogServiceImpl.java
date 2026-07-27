package com.cadence.notificationservice.service.impl;

import com.cadence.notificationservice.dto.response.NotificationLogResponse;
import com.cadence.notificationservice.dto.response.PagedResponse;
import com.cadence.notificationservice.mapper.NotificationLogMapper;
import com.cadence.notificationservice.repository.NotificationLogRepository;
import com.cadence.notificationservice.service.NotificationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NotificationLogServiceImpl implements NotificationLogService {

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationLogMapper notificationLogMapper;

    @Override
    public PagedResponse<NotificationLogResponse> listLogs(String source, Pageable pageable) {
        var page = StringUtils.hasText(source)
                ? notificationLogRepository.findAllBySourceOrderByOccurredAtDesc(source, pageable)
                : notificationLogRepository.findAllByOrderByOccurredAtDesc(pageable);
        return PagedResponse.from(page.map(notificationLogMapper::toResponse));
    }
}

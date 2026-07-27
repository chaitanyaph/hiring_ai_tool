package com.cadence.resumeparserservice.service.impl;

import com.cadence.resumeparserservice.constants.ParsingStatus;
import com.cadence.resumeparserservice.dto.response.ParsingQueueItemResponse;
import com.cadence.resumeparserservice.dto.response.ParsingQueueSummaryResponse;
import com.cadence.resumeparserservice.mapper.ParsedResumeMapper;
import com.cadence.resumeparserservice.repository.ParsedResumeRepository;
import com.cadence.resumeparserservice.service.ParserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParserQueueServiceImpl implements ParserQueueService {

    private final ParsedResumeRepository parsedResumeRepository;
    private final ParsedResumeMapper mapper;

    private static final int PROGRESS_QUEUED = 0;
    private static final int PROGRESS_EXTRACTING = 33;
    private static final int PROGRESS_PARSING_FIELDS = 66;
    private static final int PROGRESS_PARSED = 100;

    @Override
    public Page<ParsingQueueItemResponse> getQueue(ParsingStatus status, String search, Pageable pageable) {
        return parsedResumeRepository.search(status, search, pageable)
                .map(parsedResume -> {
                    ParsingQueueItemResponse item = mapper.toQueueItemResponse(parsedResume);
                    item.setProgressPercent(progressFor(parsedResume.getStatus()));
                    return item;
                });
    }

    @Override
    public ParsingQueueSummaryResponse getSummary() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return ParsingQueueSummaryResponse.builder()
                .queuedCount(parsedResumeRepository.countByStatus(ParsingStatus.QUEUED))
                .processingCount(parsedResumeRepository.countByStatus(ParsingStatus.EXTRACTING_TEXT)
                        + parsedResumeRepository.countByStatus(ParsingStatus.PARSING_FIELDS))
                .parsedTodayCount(parsedResumeRepository.countByStatusAndParsedAtAfter(ParsingStatus.PARSED, startOfToday))
                .failedCount(parsedResumeRepository.countByStatus(ParsingStatus.FAILED))
                .build();
    }

    private Integer progressFor(ParsingStatus status) {
        return switch (status) {
            case QUEUED -> PROGRESS_QUEUED;
            case EXTRACTING_TEXT -> PROGRESS_EXTRACTING;
            case PARSING_FIELDS -> PROGRESS_PARSING_FIELDS;
            case PARSED -> PROGRESS_PARSED;
            case FAILED -> null;
        };
    }
}

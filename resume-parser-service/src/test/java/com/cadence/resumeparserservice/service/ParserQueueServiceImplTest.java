package com.cadence.resumeparserservice.service;

import com.cadence.resumeparserservice.constants.ParsingStatus;
import com.cadence.resumeparserservice.dto.response.ParsingQueueItemResponse;
import com.cadence.resumeparserservice.dto.response.ParsingQueueSummaryResponse;
import com.cadence.resumeparserservice.entity.ParsedResume;
import com.cadence.resumeparserservice.mapper.ParsedResumeMapper;
import com.cadence.resumeparserservice.repository.ParsedResumeRepository;
import com.cadence.resumeparserservice.service.impl.ParserQueueServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParserQueueServiceImplTest {

    @Mock private ParsedResumeRepository parsedResumeRepository;
    @Mock private ParsedResumeMapper mapper;

    @InjectMocks
    private ParserQueueServiceImpl parserQueueService;

    private ParsedResume processingResume;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        processingResume = ParsedResume.builder()
                .resumeId(UUID.randomUUID()).candidateId(UUID.randomUUID())
                .checksum("x").status(ParsingStatus.EXTRACTING_TEXT).build();
        pageable = PageRequest.of(0, 20);
    }

    @Test
    void getQueue_shouldSetProgressPercent_forEachKnownStatus() {
        when(parsedResumeRepository.search(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(processingResume), pageable, 1));
        when(mapper.toQueueItemResponse(processingResume))
                .thenReturn(ParsingQueueItemResponse.builder().status(ParsingStatus.EXTRACTING_TEXT).build());

        var page = parserQueueService.getQueue(null, null, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getProgressPercent()).isEqualTo(33);
    }

    @Test
    void getSummary_shouldAggregateProcessingAsExtractingPlusParsingFields() {
        when(parsedResumeRepository.countByStatus(ParsingStatus.QUEUED)).thenReturn(2L);
        when(parsedResumeRepository.countByStatus(ParsingStatus.EXTRACTING_TEXT)).thenReturn(1L);
        when(parsedResumeRepository.countByStatus(ParsingStatus.PARSING_FIELDS)).thenReturn(3L);
        when(parsedResumeRepository.countByStatusAndParsedAtAfter(any(), any(LocalDateTime.class))).thenReturn(5L);
        when(parsedResumeRepository.countByStatus(ParsingStatus.FAILED)).thenReturn(4L);

        ParsingQueueSummaryResponse summary = parserQueueService.getSummary();

        assertThat(summary.getQueuedCount()).isEqualTo(2L);
        assertThat(summary.getProcessingCount()).isEqualTo(4L);
        assertThat(summary.getParsedTodayCount()).isEqualTo(5L);
        assertThat(summary.getFailedCount()).isEqualTo(4L);
    }
}

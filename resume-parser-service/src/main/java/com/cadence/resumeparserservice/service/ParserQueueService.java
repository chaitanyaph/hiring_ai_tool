package com.cadence.resumeparserservice.service;

import com.cadence.resumeparserservice.constants.ParsingStatus;
import com.cadence.resumeparserservice.dto.response.ParsingQueueItemResponse;
import com.cadence.resumeparserservice.dto.response.ParsingQueueSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Backs the #sec-parsing table + its KPI row. */
public interface ParserQueueService {

    Page<ParsingQueueItemResponse> getQueue(ParsingStatus status, String search, Pageable pageable);

    ParsingQueueSummaryResponse getSummary();
}

package com.cadence.jobservice.dto.response;

import lombok.*;

/** Backs the Jobs listing header ("18 open positions across 4 departments") and the filter tabs. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCountsResponse {
    private long total;
    private long published;
    private long draft;
    private long archived;
    private long paused;
    private long closed;
    private long expired;
    private long distinctDepartments;
}

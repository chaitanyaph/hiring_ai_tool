package com.cadence.interviewmanagementservice.dto.request;

import com.cadence.interviewmanagementservice.constants.RoundType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Matches modal-interview (§A2) exactly: Candidate/Interview type/Date/Time/Duration/Panel/Auto-generate Meet toggle/Notify toggle. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleInterviewRequest {

    @NotNull
    private UUID applicationId;

    @NotNull
    private UUID jobId;

    @NotNull
    private UUID candidateId;

    private UUID interviewRoundId;

    @NotNull
    private RoundType roundType;

    @NotNull
    private LocalDate scheduledDate;

    @NotNull
    private LocalTime scheduledTime;

    @NotNull
    @Min(15)
    private Integer durationMinutes;

    @NotEmpty
    private List<UUID> panelistIds;

    @Builder.Default
    private boolean autoGenerateMeetLink = true;

    @Builder.Default
    private boolean notifyCandidateByEmail = true;

    private String notesForPanel;
}

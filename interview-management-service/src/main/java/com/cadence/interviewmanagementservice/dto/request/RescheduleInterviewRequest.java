package com.cadence.interviewmanagementservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/** Matches modal-reschedule-interview (§A6): new date, new time, duration, optional reason. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleInterviewRequest {

    @NotNull
    private LocalDate scheduledDate;

    @NotNull
    private LocalTime scheduledTime;

    @NotNull
    @Min(15)
    private Integer durationMinutes;

    private String rescheduleReason;
}

package com.cadence.codingassessmentservice.dto.response;

import lombok.*;

/** Backs the IDE output window's "Compilation Results" tab after a Run (not scored, no test-case verdicts). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunCodeResponse {
    private String output;
    private String stderr;
    private String compileOutput;
    private Integer runtimeMs;
    private Integer memoryKb;
}

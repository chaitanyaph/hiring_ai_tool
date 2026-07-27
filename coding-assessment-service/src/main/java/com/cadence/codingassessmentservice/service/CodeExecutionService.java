package com.cadence.codingassessmentservice.service;

import com.cadence.codingassessmentservice.dto.request.RunCodeRequest;
import com.cadence.codingassessmentservice.dto.request.SubmitCodeRequest;
import com.cadence.codingassessmentservice.dto.response.RunCodeResponse;
import com.cadence.codingassessmentservice.dto.response.SubmitCodeResponse;

/** Run (unscored, single execution against sample/custom input) and Submit (scored, one execution per visible+hidden test case) -- both delegate to the active CodeExecutionProvider (Judge0), never executing candidate code in-process. */
public interface CodeExecutionService {

    RunCodeResponse runCode(RunCodeRequest request);

    SubmitCodeResponse submitCode(SubmitCodeRequest request);
}

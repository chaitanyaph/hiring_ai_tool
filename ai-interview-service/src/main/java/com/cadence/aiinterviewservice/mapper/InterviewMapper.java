package com.cadence.aiinterviewservice.mapper;

import com.cadence.aiinterviewservice.dto.response.InterviewQueueItemResponse;
import com.cadence.aiinterviewservice.dto.response.InterviewQuestionResponse;
import com.cadence.aiinterviewservice.entity.InterviewQuestion;
import com.cadence.aiinterviewservice.entity.InterviewSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** fullName/email/jobTitle come from Application Service's snapshot fields, batch-fetched once per job by the query service. */
@Mapper(componentModel = "spring")
public interface InterviewMapper {

    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "jobTitle", ignore = true)
    InterviewQueueItemResponse toQueueItemResponse(InterviewSession session);

    @Mapping(target = "questionId", source = "id")
    @Mapping(target = "totalQuestions", ignore = true)
    @Mapping(target = "interviewCompleted", ignore = true)
    InterviewQuestionResponse toQuestionResponse(InterviewQuestion question);
}

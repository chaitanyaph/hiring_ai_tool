package com.cadence.codingassessmentservice.mapper;

import com.cadence.codingassessmentservice.dto.response.AssessmentListItemResponse;
import com.cadence.codingassessmentservice.dto.response.AssessmentResponse;
import com.cadence.codingassessmentservice.entity.Assessment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.List;

/** jobTitle/invitedCount are enriched by the service layer via Feign/repository counts, not stored on the entity -- allowedLanguages is stored as a comma-separated column and split here. */
@Mapper(componentModel = "spring")
public interface AssessmentMapper {

    @Mapping(target = "jobTitle", ignore = true)
    @Mapping(target = "allowedLanguages", expression = "java(splitLanguages(assessment.getAllowedLanguages()))")
    AssessmentResponse toResponse(Assessment assessment);

    @Mapping(target = "jobTitle", ignore = true)
    @Mapping(target = "invitedCount", ignore = true)
    @Mapping(target = "allowedLanguages", expression = "java(splitLanguages(assessment.getAllowedLanguages()))")
    AssessmentListItemResponse toListItemResponse(Assessment assessment);

    default List<String> splitLanguages(String allowedLanguages) {
        if (allowedLanguages == null || allowedLanguages.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedLanguages.split(",")).map(String::trim).toList();
    }

    default String joinLanguages(List<?> languages) {
        if (languages == null || languages.isEmpty()) {
            return "";
        }
        return String.join(",", languages.stream().map(String::valueOf).toList());
    }
}

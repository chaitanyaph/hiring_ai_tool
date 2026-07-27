package com.cadence.codingassessmentservice.mapper;

import com.cadence.codingassessmentservice.dto.response.QuestionResponse;
import com.cadence.codingassessmentservice.entity.Question;
import com.cadence.codingassessmentservice.entity.QuestionTestCase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.List;

/** starterCodes/testCases/hiddenTestCaseCount are assembled by the service layer from their own child-table queries, not a JPA object graph -- same "plain UUID FK, no relationship" style as every sibling service. tags/allowedLanguages are comma-separated columns split here. */
@Mapper(componentModel = "spring")
public interface QuestionMapper {

    @Mapping(target = "tags", expression = "java(splitCsv(question.getTags()))")
    @Mapping(target = "allowedLanguages", expression = "java(splitCsv(question.getAllowedLanguages()))")
    @Mapping(target = "starterCodes", ignore = true)
    @Mapping(target = "testCases", ignore = true)
    @Mapping(target = "hiddenTestCaseCount", ignore = true)
    QuestionResponse toResponse(Question question);

    QuestionResponse.TestCaseResponse toTestCaseResponse(QuestionTestCase testCase);
    List<QuestionResponse.TestCaseResponse> toTestCaseResponseList(List<QuestionTestCase> testCases);

    default List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).toList();
    }

    default String joinCsv(List<?> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(",", values.stream().map(String::valueOf).toList());
    }
}

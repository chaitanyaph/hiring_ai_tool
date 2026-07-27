package com.cadence.codingassessmentservice.strategy;

import com.cadence.codingassessmentservice.constants.SubmissionStatus;
import com.cadence.codingassessmentservice.execution.CodeExecutionProvider;
import com.cadence.codingassessmentservice.execution.ExecutionRequest;
import com.cadence.codingassessmentservice.execution.ExecutionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeExecutionProviderFactoryTest {

    private CodeExecutionProvider fakeProvider(String name) {
        return new CodeExecutionProvider() {
            @Override
            public ExecutionResult execute(ExecutionRequest request) {
                return new ExecutionResult(SubmissionStatus.ACCEPTED, "", null, null, 10, 100);
            }

            @Override
            public String getProviderName() {
                return name;
            }
        };
    }

    @Test
    void getActiveProvider_shouldReturnConfiguredProvider_caseInsensitively() {
        CodeExecutionProvider judge0 = fakeProvider("JUDGE0");
        CodeExecutionProviderFactory factory = new CodeExecutionProviderFactory(List.of(judge0), "judge0");

        assertThat(factory.getActiveProvider()).isSameAs(judge0);
    }

    @Test
    void getActiveProvider_shouldThrow_whenConfiguredProviderIsUnknown() {
        CodeExecutionProvider judge0 = fakeProvider("JUDGE0");
        CodeExecutionProviderFactory factory = new CodeExecutionProviderFactory(List.of(judge0), "not-a-real-provider");

        assertThatThrownBy(factory::getActiveProvider).isInstanceOf(IllegalStateException.class);
    }
}

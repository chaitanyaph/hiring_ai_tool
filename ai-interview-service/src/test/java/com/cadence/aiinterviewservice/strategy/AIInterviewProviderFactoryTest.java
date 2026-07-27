package com.cadence.aiinterviewservice.strategy;

import com.cadence.aiinterviewservice.provider.AIInterviewProvider;
import com.cadence.aiinterviewservice.provider.GeneratedQuestion;
import com.cadence.aiinterviewservice.provider.InterviewEvaluationContext;
import com.cadence.aiinterviewservice.provider.InterviewEvaluationData;
import com.cadence.aiinterviewservice.provider.InterviewQuestionContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AIInterviewProviderFactoryTest {

    private AIInterviewProvider fakeProvider(String name) {
        return new AIInterviewProvider() {
            @Override
            public GeneratedQuestion generateNextQuestion(InterviewQuestionContext context) {
                return null;
            }

            @Override
            public InterviewEvaluationData evaluateInterview(InterviewEvaluationContext context) {
                return null;
            }

            @Override
            public String getProviderName() {
                return name;
            }
        };
    }

    @Test
    void getActiveProvider_shouldReturnConfiguredProvider_caseInsensitively() {
        AIInterviewProvider gemini = fakeProvider("GEMINI");
        AIInterviewProvider groq = fakeProvider("GROQ");
        AIInterviewProviderFactory factory = new AIInterviewProviderFactory(List.of(gemini, groq), "gemini");

        assertThat(factory.getActiveProvider()).isSameAs(gemini);
    }

    @Test
    void getActiveProvider_shouldThrow_whenConfiguredProviderIsUnknown() {
        AIInterviewProvider gemini = fakeProvider("GEMINI");
        AIInterviewProviderFactory factory = new AIInterviewProviderFactory(List.of(gemini), "not-a-real-provider");

        assertThatThrownBy(factory::getActiveProvider).isInstanceOf(IllegalStateException.class);
    }
}

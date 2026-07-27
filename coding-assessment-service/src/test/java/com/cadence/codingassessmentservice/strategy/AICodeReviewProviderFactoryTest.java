package com.cadence.codingassessmentservice.strategy;

import com.cadence.codingassessmentservice.review.AICodeReviewProvider;
import com.cadence.codingassessmentservice.review.CodeReviewContext;
import com.cadence.codingassessmentservice.review.CodeReviewData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AICodeReviewProviderFactoryTest {

    private AICodeReviewProvider fakeProvider(String name) {
        return new AICodeReviewProvider() {
            @Override
            public CodeReviewData reviewCode(CodeReviewContext context) {
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
        AICodeReviewProvider gemini = fakeProvider("GEMINI");
        AICodeReviewProvider groq = fakeProvider("GROQ");
        AICodeReviewProviderFactory factory = new AICodeReviewProviderFactory(List.of(gemini, groq), "gemini");

        assertThat(factory.getActiveProvider()).isSameAs(gemini);
    }

    @Test
    void getActiveProvider_shouldThrow_whenConfiguredProviderIsUnknown() {
        AICodeReviewProvider gemini = fakeProvider("GEMINI");
        AICodeReviewProviderFactory factory = new AICodeReviewProviderFactory(List.of(gemini), "not-a-real-provider");

        assertThatThrownBy(factory::getActiveProvider).isInstanceOf(IllegalStateException.class);
    }
}

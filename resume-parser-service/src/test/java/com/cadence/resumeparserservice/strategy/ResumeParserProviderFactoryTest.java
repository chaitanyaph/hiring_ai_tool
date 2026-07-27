package com.cadence.resumeparserservice.strategy;

import com.cadence.resumeparserservice.provider.JobRequirementsSnapshot;
import com.cadence.resumeparserservice.provider.MatchAnalysisData;
import com.cadence.resumeparserservice.provider.ParsedResumeData;
import com.cadence.resumeparserservice.provider.ParsedResumeSnapshot;
import com.cadence.resumeparserservice.provider.ResumeParserProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumeParserProviderFactoryTest {

    private ResumeParserProvider fakeProvider(String name) {
        return new ResumeParserProvider() {
            @Override
            public ParsedResumeData parse(String resumeText) {
                return null;
            }

            @Override
            public MatchAnalysisData analyzeMatch(ParsedResumeSnapshot resume, JobRequirementsSnapshot job) {
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
        ResumeParserProvider gemini = fakeProvider("GEMINI");
        ResumeParserProvider groq = fakeProvider("GROQ");
        ResumeParserProviderFactory factory = new ResumeParserProviderFactory(List.of(gemini, groq), "gemini");

        assertThat(factory.getActiveProvider()).isSameAs(gemini);
    }

    @Test
    void getActiveProvider_shouldThrow_whenConfiguredProviderIsUnknown() {
        ResumeParserProvider gemini = fakeProvider("GEMINI");
        ResumeParserProviderFactory factory = new ResumeParserProviderFactory(List.of(gemini), "not-a-real-provider");

        assertThatThrownBy(factory::getActiveProvider).isInstanceOf(IllegalStateException.class);
    }
}

package com.cadence.aiinterviewservice.provider;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Google Cloud Text-to-Speech REST API (texttospeech.googleapis.com), called
 * with a plain API key the same way GeminiInterviewProvider calls
 * generateContent -- reuses ai-interview.ai.gemini.api-key by default since
 * Google AI Studio keys are Google Cloud API keys and Cloud TTS accepts
 * key-based auth for this endpoint. If that project doesn't have the
 * "Cloud Text-to-Speech API" enabled, synthesis fails closed (logged,
 * returns null) rather than breaking the interview -- enable it in Google
 * Cloud Console for that project, or point ai-interview.tts.api-key at a
 * separate key that does, if that's ever needed.
 */
@Slf4j
@Component
public class GoogleTextToSpeechServiceImpl implements TextToSpeechService {

    private final RestClient restClient;
    private final String apiKey;
    private final String languageCode;
    private final String voiceName;
    private final boolean enabled;

    public GoogleTextToSpeechServiceImpl(
            @Value("${ai-interview.tts.enabled:true}") boolean enabled,
            @Value("${ai-interview.tts.base-url:https://texttospeech.googleapis.com/v1}") String baseUrl,
            @Value("${ai-interview.tts.api-key:}") String apiKey,
            @Value("${ai-interview.tts.language-code:en-US}") String languageCode,
            @Value("${ai-interview.tts.voice-name:en-US-Standard-C}") String voiceName,
            @Value("${ai-interview.tts.timeout-seconds:15}") int timeoutSeconds) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.languageCode = languageCode;
        this.voiceName = voiceName;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutSeconds * 1000);
        requestFactory.setReadTimeout(timeoutSeconds * 1000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public String synthesize(String text) {
        if (!enabled || apiKey == null || apiKey.isBlank() || text == null || text.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> requestBody = Map.of(
                    "input", Map.of("text", text),
                    "voice", Map.of("languageCode", languageCode, "name", voiceName),
                    "audioConfig", Map.of("audioEncoding", "MP3")
            );
            JsonNode response = restClient.post()
                    .uri("/text:synthesize?key={key}", apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
            String audioContent = response != null ? response.path("audioContent").asText(null) : null;
            if (audioContent == null || audioContent.isBlank()) {
                log.warn("Text-to-speech returned no audio content");
                return null;
            }
            return audioContent;
        } catch (Exception e) {
            log.warn("Text-to-speech synthesis failed, continuing without audio: {}", e.getMessage());
            return null;
        }
    }
}

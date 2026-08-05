package com.cadence.aiinterviewservice.provider;

/** Synthesizes speech audio for a question so it can actually be spoken to the candidate, not just displayed as text. Speech-to-text (the candidate's answer) stays client-side (Web Speech API) -- this is the other half, server-side text-to-speech for the AI's questions. */
public interface TextToSpeechService {

    /**
     * Returns base64-encoded audio content (MP3), or null if synthesis is
     * disabled or fails -- TTS is a non-essential enhancement, never allowed
     * to block the interview question/answer loop.
     */
    String synthesize(String text);
}

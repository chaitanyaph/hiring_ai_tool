package com.cadence.aiinterviewservice.provider;

/** One already-asked question + the candidate's answer -- given to the provider as prior context so follow-up questions aren't asked in a vacuum. */
public record QaPair(String questionText, String answerText) {}

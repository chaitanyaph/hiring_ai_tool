package com.cadence.resumeparserservice.util;

import org.springframework.stereotype.Component;

/** Normalizes PDFBox's raw extraction output before it's handed to an LLM -- collapses excess whitespace and strips control characters. */
@Component
public class TextCleaner {

    public String clean(String rawText) {
        return rawText
                .replace("\r\n", "\n")
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}

package com.cadence.resumeservice.service;

import java.io.InputStream;

/** Transport-only holder between the service and controller layers for a streamed download/preview -- never serialized as JSON. */
public record ResumeContent(InputStream content, String fileName, String mimeType, long fileSize) {
}

package com.cadence.resumeparserservice.constants;

/**
 * Maps 1:1 onto the Figma drawer's 4-step processing stepper
 * (Uploaded / Extracting text / Parsing fields / Complete), with
 * FAILED reachable from any of the three in-flight states.
 */
public enum ParsingStatus {
    QUEUED,
    EXTRACTING_TEXT,
    PARSING_FIELDS,
    PARSED,
    FAILED
}

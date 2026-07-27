package com.cadence.resumeparserservice.constants;

/**
 * Deliberately one normalized table (candidate_skill) with this
 * category discriminator, rather than eight separate near-identical
 * tables (Technical/Soft/ProgrammingLanguage/Framework/Library/
 * Database/Cloud/DevOps) -- the extraction spec calls out eight skill
 * buckets, but they're all structurally the same fact ("this candidate
 * has this skill"), so splitting them into physically separate tables
 * would be over-normalization, not correctness.
 */
public enum SkillCategory {
    TECHNICAL,
    SOFT,
    PROGRAMMING_LANGUAGE,
    FRAMEWORK,
    LIBRARY,
    DATABASE,
    CLOUD,
    DEVOPS
}

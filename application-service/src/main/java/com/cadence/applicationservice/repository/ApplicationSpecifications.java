package com.cadence.applicationservice.repository;

import com.cadence.applicationservice.constant.ApplicationStage;
import com.cadence.applicationservice.constant.ApplicationStatus;
import com.cadence.applicationservice.constant.Priority;
import com.cadence.applicationservice.entity.Application;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Every filter here maps to a column that actually exists on this
 * service's own table -- Department/Location/Experience filters from
 * the product spec are NOT implemented, since those are Job/Candidate
 * Service concepts this service deliberately doesn't join across or
 * duplicate beyond the name/email/title snapshots already needed for
 * display. See README for the documented gap and how to close it
 * (either a Job Service pre-filter to a jobId list, or adding more
 * snapshot columns if this becomes a real product need).
 */
public final class ApplicationSpecifications {
    private ApplicationSpecifications() {}

    public static Specification<Application> companyId(UUID companyId) {
        return (root, query, cb) -> companyId == null ? null : cb.equal(root.get("companyId"), companyId);
    }

    public static Specification<Application> jobId(UUID jobId) {
        return (root, query, cb) -> jobId == null ? null : cb.equal(root.get("jobId"), jobId);
    }

    public static Specification<Application> candidateId(UUID candidateId) {
        return (root, query, cb) -> candidateId == null ? null : cb.equal(root.get("candidateId"), candidateId);
    }

    public static Specification<Application> recruiterId(UUID recruiterId) {
        return (root, query, cb) -> recruiterId == null ? null : cb.equal(root.get("assignedRecruiterId"), recruiterId);
    }

    public static Specification<Application> hiringManagerId(UUID hiringManagerId) {
        return (root, query, cb) -> hiringManagerId == null ? null : cb.equal(root.get("assignedHiringManagerId"), hiringManagerId);
    }

    public static Specification<Application> status(ApplicationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("currentStatus"), status);
    }

    public static Specification<Application> stage(ApplicationStage stage) {
        return (root, query, cb) -> stage == null ? null : cb.equal(root.get("currentStage"), stage);
    }

    public static Specification<Application> priority(Priority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Application> candidateNameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank()) ? null
                : cb.like(cb.lower(root.get("candidateNameSnapshot")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Application> candidateEmailContains(String email) {
        return (root, query, cb) -> (email == null || email.isBlank()) ? null
                : cb.like(cb.lower(root.get("candidateEmailSnapshot")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<Application> jobTitleContains(String title) {
        return (root, query, cb) -> (title == null || title.isBlank()) ? null
                : cb.like(cb.lower(root.get("jobTitleSnapshot")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Application> minOverallScore(Integer minScore) {
        return (root, query, cb) -> minScore == null ? null : cb.greaterThanOrEqualTo(root.get("overallScore"), minScore);
    }

    public static Specification<Application> appliedBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("appliedAt"), from, to);
            return from != null ? cb.greaterThanOrEqualTo(root.get("appliedAt"), from)
                    : cb.lessThanOrEqualTo(root.get("appliedAt"), to);
        };
    }
}

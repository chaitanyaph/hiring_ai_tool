package com.cadence.jobservice.repository;

import com.cadence.jobservice.constant.EmploymentType;
import com.cadence.jobservice.constant.JobStatus;
import com.cadence.jobservice.entity.Job;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Builds the Jobs-listing search/filter query dynamically -- title,
 * department, location, status, recruiter, hiring manager, employment
 * type and date range are all optional and combinable, which a fixed
 * set of derived repository methods can't express without an explosion
 * of method names.
 */
public final class JobSpecifications {
    private JobSpecifications() {}

    public static Specification<Job> companyId(UUID companyId) {
        return (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    }

    public static Specification<Job> titleContains(String title) {
        return (root, query, cb) -> title == null || title.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Job> departmentId(UUID departmentId) {
        return (root, query, cb) -> departmentId == null
                ? cb.conjunction()
                : cb.equal(root.get("departmentId"), departmentId);
    }

    public static Specification<Job> locationContains(String location) {
        return (root, query, cb) -> location == null || location.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
    }

    public static Specification<Job> status(JobStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Job> employmentType(EmploymentType employmentType) {
        return (root, query, cb) -> employmentType == null
                ? cb.conjunction()
                : cb.equal(root.get("employmentType"), employmentType);
    }

    public static Specification<Job> recruiterId(UUID recruiterId) {
        return (root, query, cb) -> recruiterId == null
                ? cb.conjunction()
                : cb.equal(root.get("recruiterId"), recruiterId);
    }

    public static Specification<Job> hiringManagerId(UUID hiringManagerId) {
        return (root, query, cb) -> hiringManagerId == null
                ? cb.conjunction()
                : cb.equal(root.get("hiringManagerId"), hiringManagerId);
    }

    public static Specification<Job> createdBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from.atStartOfDay(), to.plusDays(1).atStartOfDay());
            }
            return from != null
                    ? cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay())
                    : cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay());
        };
    }
}

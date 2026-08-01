package com.cadence.companyservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("is_deleted = false")
public class Company extends BaseAuditEntity implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "company_slug", nullable = false, unique = true, length = 160)
    private String companySlug;

    @Column(length = 120)
    private String industry;

    @Column(length = 255)
    private String website;

    @Column(name = "company_email", length = 180)
    private String companyEmail;

    @Column(name = "company_phone", length = 30)
    private String companyPhone;

    @Column(length = 200)
    private String headquarters;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "company_logo", length = 500)
    private String companyLogo;

    @Builder.Default
    @Column(name = "subscription_plan", nullable = false, length = 60)
    private String subscriptionPlan = "FREE";
}

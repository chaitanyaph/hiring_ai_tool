package com.cadence.companyservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "offices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("is_deleted = false")
public class Office extends BaseAuditEntity implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "office_name", nullable = false, length = 120)
    private String officeName;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String city;

    @Column(length = 255)
    private String address;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 60)
    private String timezone;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Builder.Default
    @Column(name = "is_primary_office", nullable = false)
    private boolean primaryOffice = false;
}

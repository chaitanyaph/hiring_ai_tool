package com.cadence.companyservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("is_deleted = false")
public class Department extends BaseAuditEntity implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "department_name", nullable = false, length = 100)
    private String departmentName;

    @Column(length = 500)
    private String description;
}

package com.cadence.jobservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "job_benefits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobBenefit implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "benefit_text", nullable = false, length = 255)
    private String benefitText;
}

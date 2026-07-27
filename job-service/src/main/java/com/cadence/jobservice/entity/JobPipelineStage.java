package com.cadence.jobservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "job_pipeline_stage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPipelineStage implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "stage_name", nullable = false, length = 100)
    private String stageName;

    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(name = "is_system_default", nullable = false)
    private boolean systemDefault = false;
}

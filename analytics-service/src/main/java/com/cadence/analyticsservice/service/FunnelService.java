package com.cadence.analyticsservice.service;

import com.cadence.analyticsservice.dto.response.FunnelResponse;

import java.util.UUID;

/**
 * Stage set and order come purely from ingested FUNNEL_STAGE dimension rows (largest count
 * first) -- see FunnelServiceImpl. No job-level funnel is exposed: FUNNEL_STAGE is never
 * ingested at JOB scope (see MetricIngestionServiceImpl#onApplicationStatusChanged), so a
 * job-filtered funnel isn't backed by any ingested data.
 */
public interface FunnelService {

    FunnelResponse getGlobalFunnel();

    FunnelResponse getCompanyFunnel(UUID companyId);
}

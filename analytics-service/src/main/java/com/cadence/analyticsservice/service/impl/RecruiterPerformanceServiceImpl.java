package com.cadence.analyticsservice.service.impl;

import com.cadence.analyticsservice.constants.PeriodType;
import com.cadence.analyticsservice.dto.response.RecruiterPerformanceResponse;
import com.cadence.analyticsservice.mapper.RecruiterPerformanceMapper;
import com.cadence.analyticsservice.repository.RecruiterPerformanceSnapshotRepository;
import com.cadence.analyticsservice.service.RecruiterPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruiterPerformanceServiceImpl implements RecruiterPerformanceService {

    private final RecruiterPerformanceSnapshotRepository recruiterPerformanceSnapshotRepository;
    private final RecruiterPerformanceMapper recruiterPerformanceMapper;

    @Override
    public List<RecruiterPerformanceResponse> getRecruiterPerformance(UUID companyId) {
        return recruiterPerformanceSnapshotRepository
                .findAllByCompanyIdAndPeriodDate(companyId, PeriodType.ALL_TIME_DATE)
                .stream()
                .map(recruiterPerformanceMapper::toResponse)
                .collect(Collectors.toList());
    }
}

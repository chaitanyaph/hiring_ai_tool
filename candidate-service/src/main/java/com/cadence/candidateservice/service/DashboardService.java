package com.cadence.candidateservice.service;

import com.cadence.candidateservice.dto.response.DashboardResponse;
import com.cadence.candidateservice.security.CurrentUser;

public interface DashboardService {
    DashboardResponse getDashboard(CurrentUser candidate);
}

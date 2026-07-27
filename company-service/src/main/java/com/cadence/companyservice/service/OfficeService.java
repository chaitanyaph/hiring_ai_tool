package com.cadence.companyservice.service;

import com.cadence.companyservice.dto.request.OfficeRequest;
import com.cadence.companyservice.dto.response.OfficeResponse;
import com.cadence.companyservice.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OfficeService {
    OfficeResponse createOffice(UUID companyId, OfficeRequest request, String actor);
    PagedResponse<OfficeResponse> listOffices(UUID companyId, Pageable pageable);
    OfficeResponse getOffice(UUID officeId);
    OfficeResponse updateOffice(UUID officeId, OfficeRequest request, String actor);
    void deleteOffice(UUID officeId, String actor);
}

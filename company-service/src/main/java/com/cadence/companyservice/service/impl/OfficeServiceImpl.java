package com.cadence.companyservice.service.impl;

import com.cadence.companyservice.config.RedisConfig;
import com.cadence.companyservice.dto.request.OfficeRequest;
import com.cadence.companyservice.dto.response.OfficeResponse;
import com.cadence.companyservice.dto.response.PagedResponse;
import com.cadence.companyservice.entity.Office;
import com.cadence.companyservice.exception.ErrorCode;
import com.cadence.companyservice.exception.ResourceNotFoundException;
import com.cadence.companyservice.kafka.event.OfficeCreatedEvent;
import com.cadence.companyservice.kafka.event.OfficeDeletedEvent;
import com.cadence.companyservice.kafka.event.OfficeUpdatedEvent;
import com.cadence.companyservice.kafka.producer.CompanyEventProducer;
import com.cadence.companyservice.mapper.OfficeMapper;
import com.cadence.companyservice.repository.CompanyRepository;
import com.cadence.companyservice.repository.OfficeRepository;
import com.cadence.companyservice.service.OfficeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfficeServiceImpl implements OfficeService {

    private final OfficeRepository officeRepository;
    private final CompanyRepository companyRepository;
    private final OfficeMapper officeMapper;
    private final CompanyEventProducer eventProducer;
    private final CacheManager cacheManager;

    @Override
    @CacheEvict(value = RedisConfig.OFFICE_LIST_CACHE, key = "#companyId")
    @Transactional
    public OfficeResponse createOffice(UUID companyId, OfficeRequest request, String actor) {
        ensureCompanyExists(companyId);

        if (request.isPrimaryOffice()) {
            clearExistingPrimaryOffice(companyId);
        }

        Office office = Office.builder()
                .companyId(companyId)
                .officeName(request.getOfficeName().trim())
                .country(request.getCountry())
                .state(request.getState())
                .city(request.getCity())
                .address(request.getAddress())
                .postalCode(request.getPostalCode())
                .timezone(request.getTimezone())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .primaryOffice(request.isPrimaryOffice())
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        office = officeRepository.save(office);

        eventProducer.publishOfficeCreated(OfficeCreatedEvent.builder()
                .officeId(office.getId()).companyId(companyId).officeName(office.getOfficeName())
                .isPrimaryOffice(office.isPrimaryOffice()).occurredAt(LocalDateTime.now()).build());

        return officeMapper.toResponse(office);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OfficeResponse> listOffices(UUID companyId, Pageable pageable) {
        ensureCompanyExists(companyId);
        Page<OfficeResponse> page = officeRepository.findAllByCompanyId(companyId, pageable)
                .map(officeMapper::toResponse);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public OfficeResponse getOffice(UUID officeId) {
        return officeMapper.toResponse(findOfficeOrThrow(officeId));
    }

    @Override
    @Transactional
    public OfficeResponse updateOffice(UUID officeId, OfficeRequest request, String actor) {
        Office office = findOfficeOrThrow(officeId);

        if (request.isPrimaryOffice() && !office.isPrimaryOffice()) {
            clearExistingPrimaryOffice(office.getCompanyId());
        }

        office.setOfficeName(request.getOfficeName().trim());
        office.setCountry(request.getCountry());
        office.setState(request.getState());
        office.setCity(request.getCity());
        office.setAddress(request.getAddress());
        office.setPostalCode(request.getPostalCode());
        office.setTimezone(request.getTimezone());
        office.setLatitude(request.getLatitude());
        office.setLongitude(request.getLongitude());
        office.setPrimaryOffice(request.isPrimaryOffice());
        office.setUpdatedBy(actor);
        office = officeRepository.save(office);
        evictOfficeListCache(office.getCompanyId());

        eventProducer.publishOfficeUpdated(OfficeUpdatedEvent.builder()
                .officeId(office.getId()).companyId(office.getCompanyId())
                .officeName(office.getOfficeName()).occurredAt(LocalDateTime.now()).build());

        return officeMapper.toResponse(office);
    }

    @Override
    @Transactional
    public void deleteOffice(UUID officeId, String actor) {
        Office office = findOfficeOrThrow(officeId);
        office.setUpdatedBy(actor);
        office.markDeleted();
        officeRepository.save(office);
        evictOfficeListCache(office.getCompanyId());

        eventProducer.publishOfficeDeleted(OfficeDeletedEvent.builder()
                .officeId(office.getId()).companyId(office.getCompanyId()).occurredAt(LocalDateTime.now()).build());
    }

    /**
     * "One primary office per company" is enforced here, transactionally,
     * rather than as a DB constraint -- MySQL cannot declaratively express
     * "at most one TRUE per group" without triggers.
     */
    private void clearExistingPrimaryOffice(UUID companyId) {
        officeRepository.findByCompanyIdAndPrimaryOfficeTrue(companyId).ifPresent(existing -> {
            existing.setPrimaryOffice(false);
            officeRepository.save(existing);
        });
    }

    private void evictOfficeListCache(UUID companyId) {
        Optional.ofNullable(cacheManager.getCache(RedisConfig.OFFICE_LIST_CACHE)).ifPresent(c -> c.evict(companyId));
    }

    private Office findOfficeOrThrow(UUID officeId) {
        return officeRepository.findById(officeId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.OFFICE_NOT_FOUND, "Office not found: " + officeId));
    }

    private void ensureCompanyExists(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException(ErrorCode.COMPANY_NOT_FOUND, "Company not found: " + companyId);
        }
    }
}

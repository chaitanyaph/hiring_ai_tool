package com.cadence.companyservice.service.impl;

import com.cadence.companyservice.config.RedisConfig;
import com.cadence.companyservice.dto.request.DepartmentRequest;
import com.cadence.companyservice.dto.response.DepartmentResponse;
import com.cadence.companyservice.dto.response.PagedResponse;
import com.cadence.companyservice.entity.Department;
import com.cadence.companyservice.exception.DuplicateDepartmentException;
import com.cadence.companyservice.exception.ErrorCode;
import com.cadence.companyservice.exception.ResourceNotFoundException;
import com.cadence.companyservice.kafka.event.DepartmentCreatedEvent;
import com.cadence.companyservice.kafka.event.DepartmentDeletedEvent;
import com.cadence.companyservice.kafka.event.DepartmentUpdatedEvent;
import com.cadence.companyservice.kafka.producer.CompanyEventProducer;
import com.cadence.companyservice.mapper.DepartmentMapper;
import com.cadence.companyservice.repository.CompanyRepository;
import com.cadence.companyservice.repository.DepartmentRepository;
import com.cadence.companyservice.service.DepartmentService;
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
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CompanyRepository companyRepository;
    private final DepartmentMapper departmentMapper;
    private final CompanyEventProducer eventProducer;
    private final CacheManager cacheManager;

    private void evictDepartmentListCache(UUID companyId) {
        Optional.ofNullable(cacheManager.getCache(RedisConfig.DEPARTMENT_LIST_CACHE)).ifPresent(c -> c.evict(companyId));
    }

    @Override
    @CacheEvict(value = RedisConfig.DEPARTMENT_LIST_CACHE, key = "#companyId")
    @Transactional
    public DepartmentResponse createDepartment(UUID companyId, DepartmentRequest request, String actor) {
        ensureCompanyExists(companyId);
        String name = request.getDepartmentName().trim();
        if (departmentRepository.existsByCompanyIdAndDepartmentNameIgnoreCase(companyId, name)) {
            throw new DuplicateDepartmentException(name);
        }

        Department department = Department.builder()
                .companyId(companyId)
                .departmentName(name)
                .description(request.getDescription())
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        department = departmentRepository.save(department);

        eventProducer.publishDepartmentCreated(DepartmentCreatedEvent.builder()
                .departmentId(department.getId()).companyId(companyId)
                .departmentName(department.getDepartmentName()).occurredAt(LocalDateTime.now()).build());

        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DepartmentResponse> listDepartments(UUID companyId, Pageable pageable) {
        ensureCompanyExists(companyId);
        Page<DepartmentResponse> page = departmentRepository.findAllByCompanyId(companyId, pageable)
                .map(departmentMapper::toResponse);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(UUID departmentId) {
        return departmentMapper.toResponse(findDepartmentOrThrow(departmentId));
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(UUID departmentId, DepartmentRequest request, String actor) {
        Department department = findDepartmentOrThrow(departmentId);
        String name = request.getDepartmentName().trim();
        if (!department.getDepartmentName().equalsIgnoreCase(name)
                && departmentRepository.existsByCompanyIdAndDepartmentNameIgnoreCase(department.getCompanyId(), name)) {
            throw new DuplicateDepartmentException(name);
        }
        department.setDepartmentName(name);
        department.setDescription(request.getDescription());
        department.setUpdatedBy(actor);
        department = departmentRepository.save(department);
        evictDepartmentListCache(department.getCompanyId());

        eventProducer.publishDepartmentUpdated(DepartmentUpdatedEvent.builder()
                .departmentId(department.getId()).companyId(department.getCompanyId())
                .departmentName(department.getDepartmentName()).occurredAt(LocalDateTime.now()).build());

        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional
    public void deleteDepartment(UUID departmentId, String actor) {
        Department department = findDepartmentOrThrow(departmentId);
        department.setUpdatedBy(actor);
        department.markDeleted();
        departmentRepository.save(department);
        evictDepartmentListCache(department.getCompanyId());

        eventProducer.publishDepartmentDeleted(DepartmentDeletedEvent.builder()
                .departmentId(department.getId()).companyId(department.getCompanyId())
                .occurredAt(LocalDateTime.now()).build());
    }

    private Department findDepartmentOrThrow(UUID departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DEPARTMENT_NOT_FOUND, "Department not found: " + departmentId));
    }

    private void ensureCompanyExists(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException(ErrorCode.COMPANY_NOT_FOUND, "Company not found: " + companyId);
        }
    }
}

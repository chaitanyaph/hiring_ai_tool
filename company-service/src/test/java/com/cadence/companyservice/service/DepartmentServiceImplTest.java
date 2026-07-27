package com.cadence.companyservice.service;

import com.cadence.companyservice.dto.request.DepartmentRequest;
import com.cadence.companyservice.dto.response.DepartmentResponse;
import com.cadence.companyservice.entity.Department;
import com.cadence.companyservice.exception.DuplicateDepartmentException;
import com.cadence.companyservice.exception.ResourceNotFoundException;
import com.cadence.companyservice.kafka.producer.CompanyEventProducer;
import com.cadence.companyservice.mapper.DepartmentMapper;
import com.cadence.companyservice.repository.CompanyRepository;
import com.cadence.companyservice.repository.DepartmentRepository;
import com.cadence.companyservice.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock private DepartmentRepository departmentRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private CompanyEventProducer eventProducer;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    @Test
    void createDepartment_shouldThrow_whenNameAlreadyExistsForCompany() {
        UUID companyId = UUID.randomUUID();
        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(departmentRepository.existsByCompanyIdAndDepartmentNameIgnoreCase(companyId, "Engineering")).thenReturn(true);

        DepartmentRequest request = DepartmentRequest.builder().departmentName("Engineering").build();

        assertThatThrownBy(() -> departmentService.createDepartment(companyId, request, "admin"))
                .isInstanceOf(DuplicateDepartmentException.class);

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createDepartment_shouldThrow_whenCompanyDoesNotExist() {
        UUID companyId = UUID.randomUUID();
        when(companyRepository.existsById(companyId)).thenReturn(false);

        DepartmentRequest request = DepartmentRequest.builder().departmentName("Engineering").build();

        assertThatThrownBy(() -> departmentService.createDepartment(companyId, request, "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteDepartment_shouldSoftDelete_andEvictCache() {
        UUID departmentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Department department = Department.builder().id(departmentId).companyId(companyId).departmentName("Engineering").build();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        departmentService.deleteDepartment(departmentId, "admin");

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
        verify(departmentRepository, never()).delete(any());
        verify(cache).evict(companyId);
    }

    @Test
    void updateDepartment_shouldAllowKeepingSameName() {
        UUID departmentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Department department = Department.builder().id(departmentId).companyId(companyId).departmentName("Engineering").build();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));
        when(departmentMapper.toResponse(any(Department.class))).thenReturn(DepartmentResponse.builder().build());

        DepartmentRequest request = DepartmentRequest.builder().departmentName("Engineering").description("Updated").build();
        departmentService.updateDepartment(departmentId, request, "admin");

        verify(departmentRepository, never()).existsByCompanyIdAndDepartmentNameIgnoreCase(any(), any());
    }
}

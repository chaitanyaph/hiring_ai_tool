package com.cadence.companyservice.service;

import com.cadence.companyservice.dto.request.CreateCompanyRequest;
import com.cadence.companyservice.dto.request.UpdateCompanyRequest;
import com.cadence.companyservice.dto.response.CompanyResponse;
import com.cadence.companyservice.entity.Company;
import com.cadence.companyservice.exception.DuplicateCompanyNameException;
import com.cadence.companyservice.exception.ResourceNotFoundException;
import com.cadence.companyservice.kafka.producer.CompanyEventProducer;
import com.cadence.companyservice.mapper.CompanyMapper;
import com.cadence.companyservice.repository.CompanyRepository;
import com.cadence.companyservice.service.impl.CompanyServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private CompanyMapper companyMapper;
    @Mock private CompanyEventProducer eventProducer;

    @InjectMocks
    private CompanyServiceImpl companyService;

    @Test
    void createCompany_shouldGenerateSlug_fromCompanyName() {
        CreateCompanyRequest request = CreateCompanyRequest.builder()
                .companyName("Acme Corp").industry("Software").headquarters("Pune, Maharashtra").build();

        when(companyRepository.existsByCompanyNameIgnoreCase("Acme Corp")).thenReturn(false);
        when(companyRepository.existsByCompanySlug("acme-corp")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> {
            Company c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(companyMapper.toResponse(any(Company.class)))
                .thenReturn(CompanyResponse.builder().companyName("Acme Corp").companySlug("acme-corp").build());

        CompanyResponse response = companyService.createCompany(request, "admin@acme.com");

        assertThat(response.getCompanySlug()).isEqualTo("acme-corp");

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        assertThat(captor.getValue().getCompanySlug()).isEqualTo("acme-corp");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("admin@acme.com");
        verify(eventProducer).publishCompanyCreated(any());
    }

    @Test
    void createCompany_shouldAppendSuffix_whenSlugAlreadyTaken() {
        CreateCompanyRequest request = CreateCompanyRequest.builder().companyName("Acme Corp").build();

        when(companyRepository.existsByCompanyNameIgnoreCase("Acme Corp")).thenReturn(false);
        when(companyRepository.existsByCompanySlug("acme-corp")).thenReturn(true);
        when(companyRepository.existsByCompanySlug("acme-corp-2")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));
        when(companyMapper.toResponse(any(Company.class))).thenReturn(CompanyResponse.builder().build());

        companyService.createCompany(request, "admin@acme.com");

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        assertThat(captor.getValue().getCompanySlug()).isEqualTo("acme-corp-2");
    }

    @Test
    void createCompany_shouldThrow_whenNameAlreadyExists() {
        CreateCompanyRequest request = CreateCompanyRequest.builder().companyName("Acme Corp").build();
        when(companyRepository.existsByCompanyNameIgnoreCase("Acme Corp")).thenReturn(true);

        assertThatThrownBy(() -> companyService.createCompany(request, "admin@acme.com"))
                .isInstanceOf(DuplicateCompanyNameException.class);

        verify(companyRepository, never()).save(any());
    }

    @Test
    void getCompany_shouldThrow_whenNotFound() {
        UUID companyId = UUID.randomUUID();
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getCompany(companyId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCompany_shouldOnlyChangeSubscriptionPlan_whenProvided() {
        UUID companyId = UUID.randomUUID();
        Company existing = Company.builder().id(companyId).companyName("Acme Corp").companySlug("acme-corp")
                .subscriptionPlan("FREE").build();
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(existing));
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));
        when(companyMapper.toResponse(any(Company.class))).thenReturn(CompanyResponse.builder().build());

        UpdateCompanyRequest request = UpdateCompanyRequest.builder()
                .companyName("Acme Corp").subscriptionPlan("GROWTH").build();
        companyService.updateCompany(companyId, request, "admin@acme.com");

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        assertThat(captor.getValue().getSubscriptionPlan()).isEqualTo("GROWTH");
    }

    @Test
    void deleteCompany_shouldSoftDelete_notHardDelete() {
        UUID companyId = UUID.randomUUID();
        Company existing = Company.builder().id(companyId).companyName("Acme Corp").companySlug("acme-corp").build();
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(existing));
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        companyService.deleteCompany(companyId, "admin@acme.com");

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        verify(companyRepository, never()).delete(any());
        verify(companyRepository, never()).deleteById(any());
        assertThat(captor.getValue().isDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }
}

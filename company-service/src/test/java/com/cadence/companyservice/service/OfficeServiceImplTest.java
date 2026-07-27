package com.cadence.companyservice.service;

import com.cadence.companyservice.dto.request.OfficeRequest;
import com.cadence.companyservice.dto.response.OfficeResponse;
import com.cadence.companyservice.entity.Office;
import com.cadence.companyservice.kafka.producer.CompanyEventProducer;
import com.cadence.companyservice.mapper.OfficeMapper;
import com.cadence.companyservice.repository.CompanyRepository;
import com.cadence.companyservice.repository.OfficeRepository;
import com.cadence.companyservice.service.impl.OfficeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfficeServiceImplTest {

    @Mock private OfficeRepository officeRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private OfficeMapper officeMapper;
    @Mock private CompanyEventProducer eventProducer;
    @Mock private CacheManager cacheManager;

    @InjectMocks
    private OfficeServiceImpl officeService;

    @Test
    void createOffice_shouldUnsetPreviousPrimary_whenNewOfficeIsPrimary() {
        UUID companyId = UUID.randomUUID();
        Office existingPrimary = Office.builder().id(UUID.randomUUID()).companyId(companyId)
                .officeName("Pune HQ").primaryOffice(true).build();

        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(officeRepository.findByCompanyIdAndPrimaryOfficeTrue(companyId)).thenReturn(Optional.of(existingPrimary));
        when(officeRepository.save(any(Office.class))).thenAnswer(inv -> inv.getArgument(0));
        when(officeMapper.toResponse(any(Office.class))).thenReturn(OfficeResponse.builder().build());

        OfficeRequest request = OfficeRequest.builder().officeName("Bengaluru office").isPrimaryOffice(true).build();
        officeService.createOffice(companyId, request, "admin");

        ArgumentCaptor<Office> captor = ArgumentCaptor.forClass(Office.class);
        verify(officeRepository, times(2)).save(captor.capture());
        List<Office> saved = captor.getAllValues();

        assertThat(saved.get(0)).isSameAs(existingPrimary);
        assertThat(saved.get(0).isPrimaryOffice()).isFalse();
        assertThat(saved.get(1).getOfficeName()).isEqualTo("Bengaluru office");
        assertThat(saved.get(1).isPrimaryOffice()).isTrue();
    }

    @Test
    void createOffice_shouldNotTouchPrimaryFlag_whenNewOfficeIsNotPrimary() {
        UUID companyId = UUID.randomUUID();
        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(officeRepository.save(any(Office.class))).thenAnswer(inv -> inv.getArgument(0));
        when(officeMapper.toResponse(any(Office.class))).thenReturn(OfficeResponse.builder().build());

        OfficeRequest request = OfficeRequest.builder().officeName("Bengaluru office").isPrimaryOffice(false).build();
        officeService.createOffice(companyId, request, "admin");

        verify(officeRepository, never()).findByCompanyIdAndPrimaryOfficeTrue(any());
        verify(officeRepository, times(1)).save(any());
    }
}

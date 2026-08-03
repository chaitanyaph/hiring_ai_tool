package com.cadence.companyservice.service.impl;

import com.cadence.companyservice.config.RedisConfig;
import com.cadence.companyservice.dto.request.CreateCompanyRequest;
import com.cadence.companyservice.dto.request.UpdateCompanyRequest;
import com.cadence.companyservice.dto.response.CompanyResponse;
import com.cadence.companyservice.entity.Company;
import com.cadence.companyservice.exception.DuplicateCompanyNameException;
import com.cadence.companyservice.exception.ErrorCode;
import com.cadence.companyservice.exception.ResourceNotFoundException;
import com.cadence.companyservice.kafka.event.CompanyCreatedEvent;
import com.cadence.companyservice.kafka.event.CompanyUpdatedEvent;
import com.cadence.companyservice.kafka.producer.CompanyEventProducer;
import com.cadence.companyservice.mapper.CompanyMapper;
import com.cadence.companyservice.repository.CompanyRepository;
import com.cadence.companyservice.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final CompanyEventProducer eventProducer;

    @Override
    @Transactional
    public CompanyResponse createCompany(CreateCompanyRequest request, String actor) {
        if (companyRepository.existsByCompanyNameIgnoreCase(request.getCompanyName())) {
            throw new DuplicateCompanyNameException(request.getCompanyName());
        }

        Company company = Company.builder()
                .id(UUID.randomUUID())
                .companyName(request.getCompanyName().trim())
                .companySlug(generateUniqueSlug(request.getCompanyName()))
                .industry(request.getIndustry())
                .website(request.getWebsite())
                .companyEmail(request.getCompanyEmail())
                .companyPhone(request.getCompanyPhone())
                .headquarters(request.getHeadquarters())
                .description(request.getDescription())
                .companyLogo(request.getCompanyLogo())
                .subscriptionPlan(request.getSubscriptionPlan() != null ? request.getSubscriptionPlan() : "FREE")
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        company = companyRepository.save(company);
        log.info("Company created: {} ({})", company.getCompanyName(), company.getId());

        eventProducer.publishCompanyCreated(CompanyCreatedEvent.builder()
                .companyId(company.getId())
                .companyName(company.getCompanyName())
                .companySlug(company.getCompanySlug())
                .occurredAt(LocalDateTime.now())
                .build());

        return companyMapper.toResponse(company);
    }

    @Override
    @Cacheable(value = RedisConfig.COMPANY_CACHE, key = "#companyId")
    @Transactional(readOnly = true)
    public CompanyResponse getCompany(UUID companyId) {
        return companyMapper.toResponse(findCompanyOrThrow(companyId));
    }

    @Override
    @CacheEvict(value = RedisConfig.COMPANY_CACHE, key = "#companyId")
    @Transactional
    public CompanyResponse updateCompany(UUID companyId, UpdateCompanyRequest request, String actor) {
        Company company = findCompanyOrThrow(companyId);

        if (!company.getCompanyName().equalsIgnoreCase(request.getCompanyName())
                && companyRepository.existsByCompanyNameIgnoreCase(request.getCompanyName())) {
            throw new DuplicateCompanyNameException(request.getCompanyName());
        }

        company.setCompanyName(request.getCompanyName().trim());
        company.setIndustry(request.getIndustry());
        company.setWebsite(request.getWebsite());
        company.setCompanyEmail(request.getCompanyEmail());
        company.setCompanyPhone(request.getCompanyPhone());
        company.setHeadquarters(request.getHeadquarters());
        company.setDescription(request.getDescription());
        company.setCompanyLogo(request.getCompanyLogo());
        if (request.getSubscriptionPlan() != null && !request.getSubscriptionPlan().isBlank()) {
            company.setSubscriptionPlan(request.getSubscriptionPlan());
        }
        company.setUpdatedBy(actor);

        company = companyRepository.save(company);

        eventProducer.publishCompanyUpdated(CompanyUpdatedEvent.builder()
                .companyId(company.getId())
                .companyName(company.getCompanyName())
                .occurredAt(LocalDateTime.now())
                .build());

        return companyMapper.toResponse(company);
    }

    @Override
    @CacheEvict(value = RedisConfig.COMPANY_CACHE, key = "#companyId")
    @Transactional
    public void deleteCompany(UUID companyId, String actor) {
        Company company = findCompanyOrThrow(companyId);
        company.setUpdatedBy(actor);
        company.markDeleted();
        companyRepository.save(company);
        log.info("Company soft-deleted: {} ({})", company.getCompanyName(), company.getId());
    }

    private Company findCompanyOrThrow(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMPANY_NOT_FOUND, "Company not found: " + companyId));
    }

    private String generateUniqueSlug(String companyName) {
        String base = companyName.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (base.isEmpty()) {
            base = "company";
        }
        String slug = base;
        int suffix = 2;
        while (companyRepository.existsByCompanySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }
}

package com.cadence.companyservice.controller;

import com.cadence.companyservice.dto.request.CreateCompanyRequest;
import com.cadence.companyservice.dto.response.CompanyResponse;
import com.cadence.companyservice.exception.DuplicateCompanyNameException;
import com.cadence.companyservice.service.CompanyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CompanyController.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyService companyService;

    @Test
    void createCompany_shouldReturn201_onValidRequest() throws Exception {
        CreateCompanyRequest request = CreateCompanyRequest.builder()
                .companyName("Acme Corp").industry("Software").build();
        when(companyService.createCompany(any(), anyString()))
                .thenReturn(CompanyResponse.builder().id(UUID.randomUUID()).companyName("Acme Corp").companySlug("acme-corp").build());

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "admin@acme.com")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyName").value("Acme Corp"));
    }

    @Test
    void createCompany_shouldReturn400_onBlankName() throws Exception {
        CreateCompanyRequest request = CreateCompanyRequest.builder().companyName("").build();

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void createCompany_shouldReturn400_onInvalidWebsite() throws Exception {
        CreateCompanyRequest request = CreateCompanyRequest.builder()
                .companyName("Acme Corp").website("not-a-url").build();

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCompany_shouldReturn409_whenNameAlreadyExists() throws Exception {
        CreateCompanyRequest request = CreateCompanyRequest.builder().companyName("Acme Corp").build();
        when(companyService.createCompany(any(), anyString())).thenThrow(new DuplicateCompanyNameException("Acme Corp"));

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "admin@acme.com")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_COMPANY_NAME"));
    }

    @Test
    void getCompany_shouldReturn200() throws Exception {
        UUID companyId = UUID.randomUUID();
        when(companyService.getCompany(companyId))
                .thenReturn(CompanyResponse.builder().id(companyId).companyName("Acme Corp").build());

        mockMvc.perform(get("/api/v1/companies/{id}", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("Acme Corp"));
    }
}

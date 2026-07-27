package com.cadence.companyservice.integration;

import com.cadence.companyservice.dto.request.CreateCompanyRequest;
import com.cadence.companyservice.dto.request.DepartmentRequest;
import com.cadence.companyservice.kafka.producer.CompanyEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exercises real Flyway migrations + JPA mappings against an actual
 * MySQL 8 container -- the DB-specific behaviors we've hit before in
 * this platform (soft-delete filtering, unique constraints, FK
 * cascades) don't show up against H2. Kafka/Redis are mocked out /
 * disabled here since this test is about the persistence + web layer,
 * not messaging infrastructure.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class CompanyIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("company_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.cache.type", () -> "none");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyEventProducer eventProducer;

    @Test
    void fullFlow_createCompany_thenCreateDepartment_thenListDepartments() throws Exception {
        CreateCompanyRequest companyRequest = CreateCompanyRequest.builder()
                .companyName("Integration Test Corp " + UUID.randomUUID())
                .industry("Software")
                .build();

        String companyJson = mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(companyRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.companySlug").exists())
                .andReturn().getResponse().getContentAsString();

        String companyId = objectMapper.readTree(companyJson).path("data").path("id").asText();

        DepartmentRequest departmentRequest = DepartmentRequest.builder().departmentName("Engineering").build();
        mockMvc.perform(post("/api/v1/companies/{companyId}/departments", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(departmentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.departmentName").value("Engineering"));

        mockMvc.perform(get("/api/v1/companies/{companyId}/departments", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].departmentName").value("Engineering"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // Duplicate department name within the same company must be rejected
        mockMvc.perform(post("/api/v1/companies/{companyId}/departments", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(departmentRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void duplicateCompanyName_shouldReturn409() throws Exception {
        String uniqueName = "Duplicate Test Corp " + UUID.randomUUID();
        CreateCompanyRequest request = CreateCompanyRequest.builder().companyName(uniqueName).build();

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_COMPANY_NAME"));
    }

    @Test
    void deletedCompany_shouldReturn404_onSubsequentGet() throws Exception {
        CreateCompanyRequest request = CreateCompanyRequest.builder()
                .companyName("To Be Deleted Corp " + UUID.randomUUID()).build();

        String companyJson = mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String companyId = objectMapper.readTree(companyJson).path("data").path("id").asText();

        mockMvc.perform(delete("/api/v1/companies/{id}", companyId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/companies/{id}", companyId))
                .andExpect(status().isNotFound());
    }
}

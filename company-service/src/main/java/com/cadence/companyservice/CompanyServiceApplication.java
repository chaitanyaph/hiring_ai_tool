package com.cadence.companyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableFeignClients
public class CompanyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompanyServiceApplication.class, args);
    }
}

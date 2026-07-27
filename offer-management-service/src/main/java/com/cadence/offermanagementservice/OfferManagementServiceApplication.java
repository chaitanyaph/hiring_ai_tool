package com.cadence.offermanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableRetry
@EnableFeignClients
public class OfferManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfferManagementServiceApplication.class, args);
    }
}

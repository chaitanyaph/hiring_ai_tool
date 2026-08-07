package com.cadence.codingassessmentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableRetry
@EnableScheduling
@EnableFeignClients
public class CodingAssessmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodingAssessmentServiceApplication.class, args);
    }
}

package com.cadence.interviewmanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableRetry
@EnableFeignClients
public class InterviewManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewManagementServiceApplication.class, args);
    }
}

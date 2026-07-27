package com.cadence.jobservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableFeignClients
public class JobServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobServiceApplication.class, args);
    }
}

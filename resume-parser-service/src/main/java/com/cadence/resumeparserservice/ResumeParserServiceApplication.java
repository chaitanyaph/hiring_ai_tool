package com.cadence.resumeparserservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableRetry
@EnableFeignClients
public class ResumeParserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeParserServiceApplication.class, args);
    }
}

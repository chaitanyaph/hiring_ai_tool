package com.cadence.apigatewayservice.controller;

import com.cadence.apigatewayservice.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Where every route's circuit breaker forwards to once it's open (see RouteConfig's
 * "forward:/fallback/{serviceId}" per route) -- i.e. the downstream service is down,
 * timing out, or the breaker is deliberately giving it a moment to recover. Answered
 * here at the Gateway instead of letting the request hang or bubble up a raw 502/504,
 * so the frontend always gets the same {success,message,data} shape it already knows
 * how to render, just with success=false and a message it can show the user.
 */
@Slf4j
@RestController
public class FallbackController {

    @RequestMapping(value = "/fallback/{serviceId}", method = {
            org.springframework.web.bind.annotation.RequestMethod.GET,
            org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.PUT,
            org.springframework.web.bind.annotation.RequestMethod.PATCH,
            org.springframework.web.bind.annotation.RequestMethod.DELETE
    })
    public Mono<ResponseEntity<ApiResponse<Object>>> fallback(@PathVariable String serviceId) {
        log.warn("Circuit breaker open (or downstream timeout) for {}", serviceId);
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(serviceId + " is temporarily unavailable. Please try again in a moment.")));
    }

    @GetMapping("/fallback")
    public Mono<ResponseEntity<ApiResponse<Object>>> genericFallback() {
        return fallback("This service");
    }
}

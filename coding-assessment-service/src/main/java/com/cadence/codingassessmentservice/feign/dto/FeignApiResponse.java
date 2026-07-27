package com.cadence.codingassessmentservice.feign.dto;

import lombok.*;

/** Mirrors the {success, message, data} envelope every Cadence service returns. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeignApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}

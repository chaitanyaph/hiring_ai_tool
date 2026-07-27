package com.cadence.candidateservice.client.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String fullName;
    private String email;
}

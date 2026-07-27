package com.cadence.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(length = 255)
    private String description;
}

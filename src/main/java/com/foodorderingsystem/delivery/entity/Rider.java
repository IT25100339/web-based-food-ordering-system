package com.foodorderingsystem.delivery.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "riders")
@Data
public class Rider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long riderId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Column(unique = true)
    private String phoneNumber;

    private String vehicleNumber;

    private String password = "rider123"; // simple default password for login (admin sets this on registration)

    private String status = "AVAILABLE"; // AVAILABLE, ON_DELIVERY, OFFLINE
}

package com.foodorderingsystem.delivery.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    @Column(unique = true)
    private String phoneNumber;

    @NotBlank(message = "Vehicle number is required")
    private String vehicleNumber;

    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*(),.?\":{}|<>_\\-]).{8,}$",
            message = "Password must be at least 8 characters and include a number and a special character"
    )
    private String password = "Rider@123"; // default password for login (admin can change this on registration)

    private String status = "AVAILABLE"; // AVAILABLE, ON_DELIVERY, OFFLINE

    // Forgot-password flow: a short-lived random token emailed (simulated) to reset the password
    private String resetToken;
    private java.time.LocalDateTime resetTokenExpiry;
}

package com.foodorderingsystem.customer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Entity
@Table(name = "customers")
@Data
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*(),.?\":{}|<>_\\-]).{8,}$",
            message = "Password must be at least 8 characters and include a number and a special character"
    )
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "Phone number must contain 9-15 digits only")
    private String phone;

    private String address;

    // Forgot-password flow: a short-lived random token emailed (simulated) to reset the password
    private String resetToken;
    private java.time.LocalDateTime resetTokenExpiry;
}

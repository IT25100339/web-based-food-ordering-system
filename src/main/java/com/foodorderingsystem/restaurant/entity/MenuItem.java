package com.foodorderingsystem.restaurant.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Entity
@Table(name = "menu_items")
@Data
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @NotBlank(message = "Item name is required")
    private String name;

    private String description;

    @Positive(message = "Price must be positive")
    private Double price;

    private String category; // e.g. Rice, Short Eats, Drinks, Desserts

    private String imageUrl;

    private boolean available = true;
}

package com.foodorderingsystem.delivery.repository;

import com.foodorderingsystem.delivery.entity.Rider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiderRepository extends JpaRepository<Rider, Long> {
    List<Rider> findByStatus(String status);
    Optional<Rider> findByPhoneNumberAndPassword(String phoneNumber, String password);
}

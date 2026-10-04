package com.foodorderingsystem.delivery.service;

import com.foodorderingsystem.delivery.entity.Rider;
import com.foodorderingsystem.delivery.repository.RiderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RiderService {

    @Autowired
    private RiderRepository riderRepository;

    public Rider registerRider(Rider rider) {
        if (rider.getPassword() == null || rider.getPassword().isBlank()) {
            rider.setPassword("Rider@123");
        }
        return riderRepository.save(rider);
    }

    public Rider login(String phoneNumber, String password) {
        return riderRepository.findByPhoneNumberAndPassword(phoneNumber, password).orElse(null);
    }

    public List<Rider> getAllRiders() {
        return riderRepository.findAll();
    }

    public List<Rider> getAvailableRiders() {
        return riderRepository.findByStatus("AVAILABLE");
    }

    public Rider getRiderById(Long id) {
        return riderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rider not found with id: " + id));
    }

    public Rider updateRider(Long id, Rider updated) {
        Rider rider = getRiderById(id);
        rider.setName(updated.getName());
        rider.setPhoneNumber(updated.getPhoneNumber());
        rider.setVehicleNumber(updated.getVehicleNumber());
        rider.setStatus(updated.getStatus());
        return riderRepository.save(rider);
    }

    /**
     * Rider self-service profile update: name, phone number (their login username) and
     * vehicle number, plus an optional password change (requires the current password).
     */
    public Rider updateProfile(Long id, String name, String phoneNumber, String vehicleNumber,
                               String currentPassword, String newPassword) {
        Rider rider = getRiderById(id);

        // phone number doubles as the login username, so guard against duplicates
        if (phoneNumber != null && !phoneNumber.equals(rider.getPhoneNumber())) {
            riderRepository.findByPhoneNumber(phoneNumber).ifPresent(existing -> {
                if (!existing.getRiderId().equals(id)) {
                    throw new IllegalArgumentException("That phone number is already used by another rider.");
                }
            });
        }

        rider.setName(name);
        rider.setPhoneNumber(phoneNumber);
        rider.setVehicleNumber(vehicleNumber);

        if (newPassword != null && !newPassword.isBlank()) {
            if (currentPassword == null || !currentPassword.equals(rider.getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect.");
            }
            rider.setPassword(newPassword);
        }

        return riderRepository.save(rider);
    }

    public void deleteRider(Long id) {
        riderRepository.deleteById(id);
    }

    // ---- Forgot password ----

    /**
     * Generates a random, short-lived reset token for the given phone number (if it exists) and
     * stores it against the rider. Returns the token, or null if no rider account has that phone number.
     * (Email delivery is simulated in this dev project - the confirmation page shows the reset link directly.)
     */
    public String generateResetToken(String phoneNumber) {
        Optional<Rider> found = riderRepository.findByPhoneNumber(phoneNumber);
        if (found.isEmpty()) {
            return null;
        }
        Rider rider = found.get();
        String token = UUID.randomUUID().toString();
        rider.setResetToken(token);
        rider.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        riderRepository.save(rider);
        return token;
    }

    public Optional<Rider> findByValidResetToken(String token) {
        Optional<Rider> found = riderRepository.findByResetToken(token);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Rider rider = found.get();
        if (rider.getResetTokenExpiry() == null || rider.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        return Optional.of(rider);
    }

    public void resetPassword(Rider rider, String newPassword) {
        rider.setPassword(newPassword);
        rider.setResetToken(null);
        rider.setResetTokenExpiry(null);
        riderRepository.save(rider);
    }
}

package com.foodorderingsystem.delivery.service;

import com.foodorderingsystem.delivery.entity.Rider;
import com.foodorderingsystem.delivery.repository.RiderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiderService {

    @Autowired
    private RiderRepository riderRepository;

    public Rider registerRider(Rider rider) {
        if (rider.getPassword() == null || rider.getPassword().isBlank()) {
            rider.setPassword("rider123");
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

    public void deleteRider(Long id) {
        riderRepository.deleteById(id);
    }
}

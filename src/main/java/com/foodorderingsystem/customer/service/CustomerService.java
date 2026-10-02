package com.foodorderingsystem.customer.service;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.customer.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public Customer register(Customer customer) {
        return customerRepository.save(customer);
    }

    public Customer login(String email, String password) {
        return customerRepository.findByEmailAndPassword(email, password).orElse(null);
    }

    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    public java.util.List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer adminUpdate(Long id, String name, String email, String phone, String address) {
        Customer customer = getById(id);
        customer.setName(name);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setAddress(address);
        return customerRepository.save(customer);
    }
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }
    public boolean emailExists(String email) {
        return customerRepository.findByEmail(email).isPresent();
    }

    public Customer update(Long id, Customer updated) {
        Customer customer = getById(id);
        customer.setName(updated.getName());
        customer.setPhone(updated.getPhone());
        customer.setAddress(updated.getAddress());
        return customerRepository.save(customer);
    }
    public long getTotalCustomerCount() {
        return customerRepository.count();
    }

    /**
     * Updates profile fields, and optionally the password if a new one was supplied
     * and it matches the current password (basic re-auth check before allowing the change).
     */
    public Customer updateProfile(Long id, String name, String phone, String address,
                                  String currentPassword, String newPassword) {
        Customer customer = getById(id);
        customer.setName(name);
        customer.setPhone(phone);
        customer.setAddress(address);

        if (newPassword != null && !newPassword.isBlank()) {
            if (currentPassword == null || !currentPassword.equals(customer.getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect.");
            }
            customer.setPassword(newPassword);
        }

        return customerRepository.save(customer);
    }
    // ---- Forgot password ----

    /**
     * Generates a random, short-lived reset token for the given email (if it exists) and stores it
     * against the customer. Returns the token, or null if no account exists for that email.
     * (Email delivery is simulated in this dev project - the confirmation page shows the reset link directly.)
     */
    public String generateResetToken(String email) {
        Optional<Customer> found = customerRepository.findByEmail(email);
        if (found.isEmpty()) {
            return null;
        }
        Customer customer = found.get();
        String token = UUID.randomUUID().toString();
        customer.setResetToken(token);
        customer.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        customerRepository.save(customer);
        return token;
    }

    public Optional<Customer> findByValidResetToken(String token) {
        Optional<Customer> found = customerRepository.findByResetToken(token);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Customer customer = found.get();
        if (customer.getResetTokenExpiry() == null || customer.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        return Optional.of(customer);
    }

    public void resetPassword(Customer customer, String newPassword) {
        customer.setPassword(newPassword);
        customer.setResetToken(null);
        customer.setResetTokenExpiry(null);
        customerRepository.save(customer);
    }
}

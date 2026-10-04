package com.foodorderingsystem.customer.service;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.customer.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public long getTotalCustomerCount() {
        return customerRepository.count();
    }

    public Customer update(Long id, Customer updated) {
        Customer customer = getById(id);
        customer.setName(updated.getName());
        customer.setPhone(updated.getPhone());
        customer.setAddress(updated.getAddress());
        return customerRepository.save(customer);
    }
}

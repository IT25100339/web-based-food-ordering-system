package com.foodorderingsystem.order.service;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.entity.OrderItem;
import com.foodorderingsystem.order.repository.OrderRepository;
import com.foodorderingsystem.restaurant.entity.MenuItem;
import com.foodorderingsystem.restaurant.service.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MenuItemService menuItemService;

    // Creates an Order + OrderItems from a simple cart map (itemId -> quantity)
    public Order placeOrder(Customer customer, Map<Long, Integer> cart, String deliveryAddress) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setDeliveryAddress(deliveryAddress);
        order.setStatus("PENDING");

        double total = 0.0;
        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            MenuItem item = menuItemService.getById(entry.getKey());
            int qty = entry.getValue();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(item);
            orderItem.setQuantity(qty);
            orderItem.setPriceAtOrderTime(item.getPrice());

            order.getItems().add(orderItem);
            total += item.getPrice() * qty;
        }
        order.setTotalAmount(total);

        return orderRepository.save(order);
    }

    public List<Order> getOrdersForCustomer(Customer customer) {
        return orderRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    public Order updateStatus(Long id, String status) {
        Order order = getById(id);
        order.setStatus(status);
        return orderRepository.save(order);
    }

    public Order updatePaymentStatus(Long id, String paymentStatus) {
        Order order = getById(id);
        order.setPaymentStatus(paymentStatus);
        return orderRepository.save(order);
    }

    // Count of orders per status (Pending, Confirmed, Out for Delivery, Delivered...) - used for the admin dashboard chart
    public Map<String, Long> getOrderStatusBreakdown() {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (Order order : getAllOrders()) {
            breakdown.merge(order.getStatus(), 1L, Long::sum);
        }
        return breakdown;
    }
}

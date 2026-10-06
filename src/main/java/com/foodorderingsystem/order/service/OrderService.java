package com.foodorderingsystem.order.service;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.entity.OrderItem;
import com.foodorderingsystem.order.repository.OrderItemRepository;
import com.foodorderingsystem.order.repository.OrderRepository;
import com.foodorderingsystem.restaurant.entity.MenuItem;
import com.foodorderingsystem.restaurant.service.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private OrderItemRepository orderItemRepository;

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

    /**
     * Customer-initiated cancellation. Only the owning customer can cancel their own order,
     * and only while it's still PENDING or CONFIRMED (not once the kitchen/rider has moved on it).
     */
    public Order cancelOrder(Long orderId, Long requestingCustomerId) {
        Order order = getById(orderId);

        if (order.getCustomer() == null || !order.getCustomer().getCustomerId().equals(requestingCustomerId)) {
            throw new SecurityException("You can only cancel your own orders.");
        }
        if (!"PENDING".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
            throw new IllegalStateException("This order can no longer be cancelled - it's already " + order.getStatus().toLowerCase().replace('_', ' ') + ".");
        }

        order.setStatus("CANCELLED");
        return orderRepository.save(order);
    }

    public Map<String, Long> getOrderStatusBreakdown() {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (Order order : getAllOrders()) {
            breakdown.merge(order.getStatus(), 1L, Long::sum);
        }
        return breakdown;
    }

    public Map<String, Integer> getTopSellingItems() {
        Map<String, Integer> quantityByItem = new LinkedHashMap<>();
        for (OrderItem item : orderItemRepository.findAll()) {
            if (item.getMenuItem() == null) continue;
            quantityByItem.merge(item.getMenuItem().getName(), item.getQuantity(), Integer::sum);
        }
        Map<String, Integer> top5 = new LinkedHashMap<>();
        quantityByItem.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> top5.put(e.getKey(), e.getValue()));
        return top5;
    }

    public Map<String, Long> getWeeklyOrderVolume() {
        Map<DayOfWeek, Long> counts = new java.util.EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) counts.put(d, 0L);
        for (Order order : getAllOrders()) {
            DayOfWeek day = order.getCreatedAt().getDayOfWeek();
            counts.merge(day, 1L, Long::sum);
        }
        Map<String, Long> result = new LinkedHashMap<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            String label = d.toString().substring(0, 1) + d.toString().substring(1, 3).toLowerCase();
            result.put(label, counts.get(d));
        }
        return result;
    }
}
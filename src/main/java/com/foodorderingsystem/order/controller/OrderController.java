package com.foodorderingsystem.order.controller;

import com.foodorderingsystem.customer.entity.Customer;
import com.foodorderingsystem.order.entity.Order;
import com.foodorderingsystem.order.service.OrderService;
import com.foodorderingsystem.restaurant.entity.MenuItem;
import com.foodorderingsystem.restaurant.service.MenuItemService;
import com.foodorderingsystem.delivery.service.DeliveryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private DeliveryService deliveryService;

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCart(HttpSession session) {
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cart");
        if (cart == null) {
            cart = new LinkedHashMap<>();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    @PostMapping("/add/{itemId}")
    public String addToCart(@PathVariable Long itemId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            HttpSession session) {
        if (quantity == null || quantity < 1) {
            quantity = 1;
        }
        Map<Long, Integer> cart = getCart(session);
        cart.merge(itemId, quantity, Integer::sum);
        return "redirect:/restaurant/menu";
    }

    @GetMapping("/remove/{itemId}")
    public String removeFromCart(@PathVariable Long itemId, HttpSession session) {
        Map<Long, Integer> cart = getCart(session);
        cart.remove(itemId);
        return "redirect:/order/cart";
    }

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        Map<Long, Integer> cart = getCart(session);
        Map<MenuItem, Integer> cartItems = new LinkedHashMap<>();
        double total = 0;
        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            MenuItem item = menuItemService.getById(entry.getKey());
            cartItems.put(item, entry.getValue());
            total += item.getPrice() * entry.getValue();
        }
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("total", total);
        return "order/cart";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam String deliveryAddress, HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        if (customer == null) {
            return "redirect:/customer/login";
        }
        Map<Long, Integer> cart = getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/order/cart";
        }
        Order order = orderService.placeOrder(customer, cart, deliveryAddress);
        session.setAttribute("cart", new HashMap<Long, Integer>()); // clear cart
        return "redirect:/payment/checkout/" + order.getOrderId();
    }

    @GetMapping("/history")
    public String orderHistory(HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("loggedInCustomer");
        if (customer == null) {
            return "redirect:/customer/login";
        }
        model.addAttribute("orders", orderService.getOrdersForCustomer(customer));
        return "order/history";
    }

    @GetMapping("/track/{id}")
    public String trackOrder(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getById(id));
        model.addAttribute("delivery", deliveryService.getByOrderId(id));
        return "order/track";
    }
}

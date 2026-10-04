package com.foodorderingsystem.restaurant.service;

import com.foodorderingsystem.restaurant.entity.MenuItem;
import com.foodorderingsystem.restaurant.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuItemService {

    @Autowired
    private MenuItemRepository menuItemRepository;

    public MenuItem save(MenuItem item) {
        return menuItemRepository.save(item);
    }

    public List<MenuItem> getAvailableItems() {
        return menuItemRepository.findByAvailableTrue();
    }

    public List<MenuItem> getAvailableItemsByCategory(String category) {
        return menuItemRepository.findByCategoryAndAvailableTrue(category);
    }

    public List<String> getAvailableCategories() {
        return menuItemRepository.findByAvailableTrue().stream()
                .map(MenuItem::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public List<MenuItem> getAllItems() {
        return menuItemRepository.findAll();
    }

    public MenuItem getById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found with id: " + id));
    }

    public void delete(Long id) {
        menuItemRepository.deleteById(id);
    }
}

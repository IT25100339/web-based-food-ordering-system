package com.foodorderingsystem.restaurant.controller;

import com.foodorderingsystem.restaurant.entity.MenuItem;
import com.foodorderingsystem.restaurant.service.MenuItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/restaurant")
public class RestaurantController {

    @Autowired
    private MenuItemService menuItemService;

    // Customer-facing menu browsing (optionally filtered by category)
    @GetMapping("/menu")
    public String viewMenu(@RequestParam(required = false) String category, Model model) {
        if (category != null && !category.isBlank()) {
            model.addAttribute("items", menuItemService.getAvailableItemsByCategory(category));
        } else {
            model.addAttribute("items", menuItemService.getAvailableItems());
        }
        model.addAttribute("categories", menuItemService.getAvailableCategories());
        model.addAttribute("selectedCategory", category);
        return "restaurant/menu";
    }

    // Admin: manage menu
    @GetMapping("/manage")
    public String manageMenu(Model model) {
        model.addAttribute("items", menuItemService.getAllItems());
        return "restaurant/manage";
    }

    @GetMapping("/manage/new")
    public String showAddForm(Model model) {
        model.addAttribute("item", new MenuItem());
        return "restaurant/item-form";
    }

    @GetMapping("/manage/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", menuItemService.getById(id));
        return "restaurant/item-form";
    }

    @PostMapping("/manage/save")
    public String save(@Valid @ModelAttribute("item") MenuItem item, BindingResult result) {
        if (result.hasErrors()) {
            return "restaurant/item-form";
        }
        menuItemService.save(item);
        return "redirect:/restaurant/manage";
    }

    @GetMapping("/manage/delete/{id}")
    public String delete(@PathVariable Long id) {
        menuItemService.delete(id);
        return "redirect:/restaurant/manage";
    }
}

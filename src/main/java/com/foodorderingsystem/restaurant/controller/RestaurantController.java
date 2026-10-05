package com.foodorderingsystem.restaurant.controller;

import com.foodorderingsystem.restaurant.entity.MenuItem;
import com.foodorderingsystem.restaurant.service.MenuItemService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/restaurant")
public class RestaurantController {

    @Autowired
    private MenuItemService menuItemService;

    // Menu management (everything under /manage) is admin-only; /menu stays public for customers
    private boolean notAdmin(HttpSession session) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        return isAdmin == null || !isAdmin;
    }

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
    public String manageMenu(HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("items", menuItemService.getAllItems());
        return "restaurant/manage";
    }

    @GetMapping("/manage/new")
    public String showAddForm(HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("item", new MenuItem());
        return "restaurant/item-form";
    }

    @GetMapping("/manage/edit/{id}")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("item", menuItemService.getById(id));
        return "restaurant/item-form";
    }

    @PostMapping("/manage/save")
    public String save(@Valid @ModelAttribute("item") MenuItem item, BindingResult result,
                        HttpSession session, RedirectAttributes redirectAttributes) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            return "restaurant/item-form";
        }
        boolean isNew = item.getItemId() == null;
        menuItemService.save(item);
        redirectAttributes.addFlashAttribute("success", isNew ? "Menu item added." : "Menu item updated.");
        return "redirect:/restaurant/manage";
    }

    @GetMapping("/manage/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        menuItemService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Menu item deleted.");
        return "redirect:/restaurant/manage";
    }
}

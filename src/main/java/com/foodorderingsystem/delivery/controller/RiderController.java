package com.foodorderingsystem.delivery.controller;

import com.foodorderingsystem.delivery.entity.Rider;
import com.foodorderingsystem.delivery.service.RiderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/delivery/riders")
public class RiderController {

    @Autowired
    private RiderService riderService;

    // ---- Admin: manage rider profiles ----
    @GetMapping
    public String listRiders(Model model) {
        model.addAttribute("riders", riderService.getAllRiders());
        return "delivery/rider-list";
    }

    @GetMapping("/new")
    public String showRegisterForm(Model model) {
        model.addAttribute("rider", new Rider());
        return "delivery/rider-form";
    }

    @PostMapping("/save")
    public String saveRider(@Valid @ModelAttribute("rider") Rider rider, BindingResult result) {
        if (result.hasErrors()) {
            return "delivery/rider-form";
        }
        riderService.registerRider(rider);
        return "redirect:/delivery/riders";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("rider", riderService.getRiderById(id));
        return "delivery/rider-form";
    }

    @GetMapping("/delete/{id}")
    public String deleteRider(@PathVariable Long id) {
        riderService.deleteRider(id);
        return "redirect:/delivery/riders";
    }
}

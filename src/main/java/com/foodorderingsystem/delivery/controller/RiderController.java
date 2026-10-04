package com.foodorderingsystem.delivery.controller;

import com.foodorderingsystem.delivery.entity.Rider;
import com.foodorderingsystem.delivery.service.RiderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/delivery/riders")
public class RiderController {

    @Autowired
    private RiderService riderService;

    // This whole controller is admin-only (managing rider accounts)
    private boolean notAdmin(HttpSession session) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        return isAdmin == null || !isAdmin;
    }

    // ---- Admin: manage rider profiles ----
    @GetMapping
    public String listRiders(HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("riders", riderService.getAllRiders());
        return "delivery/rider-list";
    }

    @GetMapping("/new")
    public String showRegisterForm(HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("rider", new Rider());
        return "delivery/rider-form";
    }

    @PostMapping("/save")
    public String saveRider(@Valid @ModelAttribute("rider") Rider rider, BindingResult result,
                            HttpSession session, RedirectAttributes redirectAttributes) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            return "delivery/rider-form";
        }
        boolean isNew = rider.getRiderId() == null;
        try {
            riderService.registerRider(rider);
        } catch (DataIntegrityViolationException ex) {
            // phoneNumber has a unique constraint (it doubles as the rider's login username)
            result.rejectValue("phoneNumber", "duplicate", "This phone number is already used by another rider.");
            return "delivery/rider-form";
        }
        redirectAttributes.addFlashAttribute("success", isNew ? "Rider added." : "Rider details updated.");
        return "redirect:/delivery/riders";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("rider", riderService.getRiderById(id));
        return "delivery/rider-form";
    }

    @GetMapping("/delete/{id}")
    public String deleteRider(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (notAdmin(session)) {
            return "redirect:/login";
        }
        try {
            riderService.deleteRider(id);
            redirectAttributes.addFlashAttribute("success", "Rider removed.");
        } catch (DataIntegrityViolationException ex) {
            // rider has deliveries assigned to them (FK constraint) - block the delete
            redirectAttributes.addFlashAttribute("error", "Can't remove this rider - they have deliveries linked to their account.");
        }
        return "redirect:/delivery/riders";
    }
}

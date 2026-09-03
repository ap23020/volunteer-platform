package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.User;
import gr.hua.dit.ap.vmp.entities.Event;
import gr.hua.dit.ap.vmp.service.UserService;
import gr.hua.dit.ap.vmp.service.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final EventService eventService;

    public AdminController(UserService userService, EventService eventService) {
        this.userService = userService;
        this.eventService = eventService;
    }

    // Λίστα εκκρεμών χρηστών
    @GetMapping("/users/pending")
    public String listPendingUsers(Model model) {
        model.addAttribute("users", userService.getPendingUsers());
        model.addAttribute("activePage", "admin");
        return "admin/pending-users";
    }

    // Έγκριση χρήστη
    @PostMapping("/users/approve/{id}")
    public String approveUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.approveUser(id);
        redirectAttributes.addFlashAttribute("successMessage", "User approved successfully.");
        return "redirect:/admin/users/pending";
    }

    // Απόρριψη χρήστη
    @PostMapping("/users/reject/{id}")
    public String rejectUser(@PathVariable Long id,
                             @RequestParam(required = false) String reason,
                             RedirectAttributes redirectAttributes) {
        userService.rejectUser(id, reason);
        redirectAttributes.addFlashAttribute("successMessage", "User rejected.");
        return "redirect:/admin/users/pending";
    }

    // Λίστα εκκρεμών εκδηλώσεων
    @GetMapping("/events/pending")
    public String listPendingEvents(Model model) {
        model.addAttribute("events", eventService.getPendingEvents());
        model.addAttribute("activePage", "admin");
        return "admin/pending-events";
    }

    // Έγκριση εκδήλωσης
    @PostMapping("/events/approve/{id}")
    public String approveEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        eventService.approveEvent(id);
        redirectAttributes.addFlashAttribute("successMessage", "Event approved successfully.");
        return "redirect:/admin/events/pending";
    }

    // Απόρριψη εκδήλωσης
    @PostMapping("/events/reject/{id}")
    public String rejectEvent(@PathVariable Long id,
                              @RequestParam(required = false) String comment,
                              RedirectAttributes redirectAttributes) {
        eventService.rejectEvent(id, comment);
        redirectAttributes.addFlashAttribute("successMessage", "Event rejected.");
        return "redirect:/admin/events/pending";
    }
}
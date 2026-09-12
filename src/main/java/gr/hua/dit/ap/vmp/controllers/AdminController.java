package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.Organization;
import gr.hua.dit.ap.vmp.entities.User;
import gr.hua.dit.ap.vmp.entities.Event;
import gr.hua.dit.ap.vmp.service.OrganizationService;
import gr.hua.dit.ap.vmp.service.ReviewService;
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
    private final OrganizationService organizationService;
    private final ReviewService reviewService;
    public AdminController(UserService userService,
                           EventService eventService,
                           OrganizationService organizationService,
                           ReviewService reviewService) {
        this.userService = userService;
        this.eventService = eventService;
        this.organizationService = organizationService;
        this.reviewService = reviewService;
    }

    // ===== Διαχείριση Χρηστών =====

    // Λίστα εκκρεμών χρηστών
    @GetMapping("/users/pending")
    public String listPendingUsers(Model model) {
        model.addAttribute("users", userService.getPendingUsers());
        model.addAttribute("activePage", "adminUsers");
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

    // ===== Διαχείριση Εκδηλώσεων =====

    // Λίστα εκκρεμών εκδηλώσεων
    @GetMapping("/events/pending")
    public String listPendingEvents(Model model) {
        model.addAttribute("events", eventService.getPendingEvents());
        model.addAttribute("activePage", "adminEvents");
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

    // ===== Διαχείριση Οργανισμών =====

    // Λίστα εκκρεμών οργανισμών
    @GetMapping("/organizations/pending")
    public String listPendingOrganizations(Model model) {
        model.addAttribute("organizations", organizationService.getPendingOrganizations());
        model.addAttribute("activePage", "adminOrganizations");
        return "admin/pending-organizations";
    }

    // Έγκριση οργανισμού
    @PostMapping("/organizations/approve/{id}")
    public String approveOrganization(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        organizationService.approveOrganization(id);
        redirectAttributes.addFlashAttribute("successMessage", "Organization approved successfully.");
        return "redirect:/admin/organizations/pending";
    }

    // Απόρριψη οργανισμού (προαιρετικά με αιτιολογία)
    @PostMapping("/organizations/reject/{id}")
    public String rejectOrganization(@PathVariable Long id,
                                     @RequestParam(required = false) String reason,
                                     RedirectAttributes redirectAttributes) {
        organizationService.rejectOrganization(id, reason);
        redirectAttributes.addFlashAttribute("successMessage", "Organization rejected.");
        return "redirect:/admin/organizations/pending";
    }

    @PostMapping("/reviews/hide/{id}")
    public String hideReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reviewService.hideReview(id);
        redirectAttributes.addFlashAttribute("successMessage", "Review hidden successfully.");
        return "redirect:/review/list";
    }

    @PostMapping("/reviews/unhide/{id}")
    public String unhideReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reviewService.unhideReview(id);
        redirectAttributes.addFlashAttribute("successMessage", "Review is now visible.");
        return "redirect:/review/list";
    }

    @PostMapping("/reviews/delete/{id}")
    public String deleteReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reviewService.deleteReview(id);
        redirectAttributes.addFlashAttribute("successMessage", "Review deleted successfully.");
        return "redirect:/review/list";
    }
}
package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.*;
import gr.hua.dit.ap.vmp.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/event")
public class EventController {

    private final EventService eventService;
    private final OrganizationService organizationService;
    private final VolunteerService volunteerService;
    private final ParticipationService participationService;
    private final UserService userService;

    public EventController(EventService eventService,
                           OrganizationService organizationService,
                           VolunteerService volunteerService,
                           ParticipationService participationService,
                           UserService userService) {
        this.eventService = eventService;
        this.organizationService = organizationService;
        this.volunteerService = volunteerService;
        this.participationService = participationService;
        this.userService = userService;
    }

    // Λίστα events
    @GetMapping("/list")
    public String listEvents(Model model) {
        List<Event> events = eventService.getEvents();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isVolunteer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VOLUNTEER"));

        Set<Long> appliedEventIds = new HashSet<>();
        if (isVolunteer) {
            String email = auth.getName();
            Volunteer currentVolunteer = volunteerService.getVolunteerByEmail(email);
            if (currentVolunteer != null) {
                for (Event event : events) {
                    if (participationService.hasActiveApplication(event.getId(), currentVolunteer.getId())) {
                        appliedEventIds.add(event.getId());
                    }
                }
            }
        }

        model.addAttribute("events", events);
        model.addAttribute("appliedEventIds", appliedEventIds);
        model.addAttribute("activePage", "events");
        return "event/events";
    }

    // Φόρμα δημιουργίας event
    @GetMapping("/new")
    public String showEventForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        boolean isOrganization = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZATION"));

        if (isOrganization) {
            User user = userService.findByEmail(email);
            if (user instanceof OrganizationUser) {
                OrganizationUser orgUser = (OrganizationUser) user;
                model.addAttribute("selectedOrganization", orgUser.getOrganization());
            }
        } else {
            // Για admin: λίστα εγκεκριμένων οργανισμών
            model.addAttribute("organizations", organizationService.getApprovedOrganizations());
        }

        model.addAttribute("event", new Event());
        model.addAttribute("activePage", "events");
        return "event/event-form";
    }

    // Δημιουργία event
    @PostMapping("/new")
    public String createEvent(@ModelAttribute("event") Event event,
                              @RequestParam(value = "organizationId", required = false) Long organizationId,
                              RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        boolean isOrganization = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZATION"));

        if (isOrganization) {
            User user = userService.findByEmail(email);
            if (user instanceof OrganizationUser) {
                OrganizationUser orgUser = (OrganizationUser) user;
                event.setOrganization(orgUser.getOrganization());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Organization not found.");
                return "redirect:/event/new";
            }
        } else {
            // Admin: χρησιμοποίησε το organizationId από τη φόρμα
            if (organizationId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please select an organization.");
                return "redirect:/event/new";
            }
            Organization org = organizationService.getOrganization(organizationId);
            if (org == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Selected organization not found.");
                return "redirect:/event/new";
            }
            event.setOrganization(org);
        }

        if (event.getDateTime() != null && event.getDateTime().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", "The event date cannot be in the past.");
            return "redirect:/event/new";
        }

        event.setStatus(EventStatus.PENDING_APPROVAL);
        eventService.saveEvent(event);
        redirectAttributes.addFlashAttribute("successMessage", "Event created successfully.");
        return "redirect:/event/list";
    }

    // Υπόλοιπες μέθοδοι (edit, cancel, delete) παραμένουν ως έχουν
}
package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.Event;
import gr.hua.dit.ap.vmp.entities.EventStatus;
import gr.hua.dit.ap.vmp.entities.Organization;
import gr.hua.dit.ap.vmp.entities.OrganizationUser;
import gr.hua.dit.ap.vmp.entities.User;
import gr.hua.dit.ap.vmp.entities.Volunteer;
import gr.hua.dit.ap.vmp.service.EventService;
import gr.hua.dit.ap.vmp.service.OrganizationService;
import gr.hua.dit.ap.vmp.service.ParticipationService;
import gr.hua.dit.ap.vmp.service.UserService;
import gr.hua.dit.ap.vmp.service.VolunteerService;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        boolean isOrganization = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZATION"));
        boolean isVolunteer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VOLUNTEER"));

        List<Event> events;

        if (isOrganization) {
            // Οργανισμός: μόνο τα δικά του events
            User user = userService.findByEmail(email);
            if (user instanceof OrganizationUser) {
                OrganizationUser orgUser = (OrganizationUser) user;
                events = eventService.getEventsByOrganization(orgUser.getOrganization().getId());
            } else {
                events = List.of();
            }
        } else if (isVolunteer) {
            // Εθελοντής: μόνο εγκεκριμένα events
            events = eventService.getApprovedEvents();
        } else {
            // Admin: όλα
            events = eventService.getEvents();
        }

        Set<Long> appliedEventIds = new HashSet<>();
        if (isVolunteer) {
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

    // Φόρμα νέου event
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

    // Φόρμα επεξεργασίας
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Event event = eventService.getEvent(id);
        if (event == null) {
            return "redirect:/event/list";
        }

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
            model.addAttribute("organizations", organizationService.getApprovedOrganizations());
        }

        model.addAttribute("event", event);
        model.addAttribute("activePage", "events");
        return "event/event-form";
    }

    // Ενημέρωση event
    @PostMapping("/edit/{id}")
    public String updateEvent(@PathVariable Long id,
                              @ModelAttribute("event") Event event,
                              @RequestParam(value = "organizationId", required = false) Long organizationId,
                              RedirectAttributes redirectAttributes) {

        if (event.getDateTime() != null && event.getDateTime().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", "The event date cannot be in the past.");
            return "redirect:/event/edit/" + id;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isOrganization = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZATION"));

        // Φόρτωση του οργανισμού από τη βάση (αν δόθηκε)
        if (organizationId != null && !isOrganization) {
            Organization org = organizationService.getOrganization(organizationId);
            if (org == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Selected organization not found.");
                return "redirect:/event/edit/" + id;
            }
            event.setOrganization(org);
        } else {
            // Για org_user: κρατάμε τον υπάρχοντα οργανισμό του event
            Event existing = eventService.getEvent(id);
            if (existing != null) {
                event.setOrganization(existing.getOrganization());
            }
        }

        eventService.updateEvent(id, event);
        redirectAttributes.addFlashAttribute("successMessage", "Event updated and submitted for approval.");
        return "redirect:/event/list";
    }

    // Ακύρωση event
    @PostMapping("/cancel/{id}")
    public String cancelEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            eventService.cancelEvent(id);
            redirectAttributes.addFlashAttribute("successMessage", "Event cancelled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/event/list";
    }

    // Διαγραφή event
    @PostMapping("/delete/{id}")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        eventService.deleteEvent(id);
        redirectAttributes.addFlashAttribute("successMessage", "Event deleted successfully.");
        return "redirect:/event/list";
    }
}
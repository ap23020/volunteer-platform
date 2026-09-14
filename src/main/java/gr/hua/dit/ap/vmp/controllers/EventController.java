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

    // Λίστα events με φίλτρα
    // FIX: Χρήση των νέων μεθόδων του service ανά ρόλο
    @GetMapping("/list")
    public String listEvents(@RequestParam(required = false) Long organizationId,
                             @RequestParam(required = false) EventStatus status,
                             Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        boolean isOrganization = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZATION"));
        boolean isVolunteer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VOLUNTEER"));
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Event> events;

        if (isOrganization) {
            // Οργανισμός: μόνο τα δικά του events
            User user = userService.findByEmail(email);
            if (user instanceof OrganizationUser) {
                OrganizationUser orgUser = (OrganizationUser) user;
                Long orgId = orgUser.getOrganization().getId();
                // FIX: Χρήση της νέας μεθόδου
                events = eventService.getFilteredEventsForOrganization(orgId, status);
            } else {
                events = List.of();
            }
        } else if (isVolunteer) {
            // FIX: Χρήση της νέας μεθόδου — πάντα APPROVED
            events = eventService.getFilteredEventsForVolunteer(organizationId);
        } else if (isAdmin) {
            // FIX: Χρήση της νέας μεθόδου
            events = eventService.getFilteredEventsForAdmin(organizationId, status);
        } else {
            events = List.of();
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

        // Attributes που απαιτεί το events.html
        model.addAttribute("events", events);
        model.addAttribute("appliedEventIds", appliedEventIds);
        model.addAttribute("statuses", EventStatus.values());
        model.addAttribute("selectedOrganizationId", organizationId);
        model.addAttribute("selectedStatus", status);
        if (isAdmin) {
            model.addAttribute("organizations", organizationService.getAllOrganizations());
        }
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

        // Validation πεδίων
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Title is required.");
            return "redirect:/event/new";
        }
        if (event.getDateTime() == null || event.getDateTime().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", "The event date cannot be in the past.");
            return "redirect:/event/new";
        }
        if (event.getMaxParticipants() == null || event.getMaxParticipants() < 1) {
            redirectAttributes.addFlashAttribute("errorMessage", "Max participants must be at least 1.");
            return "redirect:/event/new";
        }
        if (event.getDuration() != null && event.getDuration() < 1) {
            redirectAttributes.addFlashAttribute("errorMessage", "Duration must be at least 1 hour.");
            return "redirect:/event/new";
        }

        event.setStatus(EventStatus.PENDING_APPROVAL);
        eventService.saveEvent(event);
        redirectAttributes.addFlashAttribute("successMessage", "Event created successfully.");
        return "redirect:/event/list";
    }

    // Φόρμα επεξεργασίας
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model,
                               RedirectAttributes redirectAttributes) {
        Event event = eventService.getEvent(id);
        if (event == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Event not found.");
            return "redirect:/event/list";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        // Έλεγχος δικαιώματος πρόσβασης στη φόρμα
        if (!canManageEvent(event, email)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot edit this event.");
            return "redirect:/event/list";
        }

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

        Event existing = eventService.getEvent(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Event not found.");
            return "redirect:/event/list";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        boolean isOrganization = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZATION"));

        // Έλεγχος δικαιώματος
        if (!canManageEvent(existing, email)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot edit this event.");
            return "redirect:/event/list";
        }

        // Validation πεδίων
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Title is required.");
            return "redirect:/event/edit/" + id;
        }
        if (event.getDateTime() == null || event.getDateTime().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", "The event date cannot be in the past.");
            return "redirect:/event/edit/" + id;
        }
        if (event.getMaxParticipants() == null || event.getMaxParticipants() < 1) {
            redirectAttributes.addFlashAttribute("errorMessage", "Max participants must be at least 1.");
            return "redirect:/event/edit/" + id;
        }

        // Ορισμός οργανισμού
        if (organizationId != null && !isOrganization) {
            Organization org = organizationService.getOrganization(organizationId);
            if (org == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Selected organization not found.");
                return "redirect:/event/edit/" + id;
            }
            event.setOrganization(org);
        } else {
            // Για org_user: κρατάμε τον υπάρχοντα οργανισμό του event
            event.setOrganization(existing.getOrganization());
        }

        try {
            eventService.updateEvent(id, event);
            redirectAttributes.addFlashAttribute("successMessage", "Event updated and submitted for approval.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/event/edit/" + id;
        }
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
        Event event = eventService.getEvent(id);
        if (event == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Event not found.");
            return "redirect:/event/list";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        if (!canManageEvent(event, email)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot delete this event.");
            return "redirect:/event/list";
        }

        eventService.deleteEvent(id);
        redirectAttributes.addFlashAttribute("successMessage", "Event deleted successfully.");
        return "redirect:/event/list";
    }

    // Βοηθητική μέθοδος για έλεγχο δικαιωμάτων διαχείρισης event
    private boolean canManageEvent(Event event, String email) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return true;

        User currentUser = userService.findByEmail(email);
        if (currentUser instanceof OrganizationUser) {
            OrganizationUser orgUser = (OrganizationUser) currentUser;
            return event.getOrganization() != null
                    && event.getOrganization().getId().equals(orgUser.getOrganization().getId());
        }
        return false;
    }
}
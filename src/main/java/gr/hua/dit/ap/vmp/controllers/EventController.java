package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.Event;
import gr.hua.dit.ap.vmp.entities.Volunteer;
import gr.hua.dit.ap.vmp.service.EventService;
import gr.hua.dit.ap.vmp.service.OrganizationService;
import gr.hua.dit.ap.vmp.service.ParticipationService;
import gr.hua.dit.ap.vmp.service.VolunteerService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/event")
public class EventController {

    private final EventService eventService;
    private final OrganizationService organizationService;
    private final VolunteerService volunteerService;        // <-- προσθήκη
    private final ParticipationService participationService; // <-- προσθήκη

    public EventController(EventService eventService,
                           OrganizationService organizationService,
                           VolunteerService volunteerService,
                           ParticipationService participationService) {
        this.eventService = eventService;
        this.organizationService = organizationService;
        this.volunteerService = volunteerService;
        this.participationService = participationService;
    }

    // Λίστα όλων των events, με υπολογισμό εφαρμοσμένων events για εθελοντή
    @GetMapping("/list")
    public String listEvents(Model model) {
        List<Event> events = eventService.getEvents();

        // Βρες τον τρέχοντα χρήστη
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

    // Φόρμα δημιουργίας νέου event (μόνο οργανισμός/admin)
    @GetMapping("/new")
    public String showEventForm(Model model) {
        model.addAttribute("event", new Event());
        model.addAttribute("organizations", organizationService.getApprovedOrganizations());
        model.addAttribute("activePage", "events");
        return "event/event-form";
    }

    // Υποβολή νέου event
    @PostMapping("/new")
    public String createEvent(@ModelAttribute("event") Event event,
                              RedirectAttributes redirectAttributes) {
        // Έλεγχος παρελθοντικής ημερομηνίας
        if (event.getDateTime() != null && event.getDateTime().isBefore(java.time.LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", "The event date cannot be in the past.");
            return "redirect:/event/new";
        }

        event.setStatus(gr.hua.dit.ap.vmp.entities.EventStatus.PENDING_APPROVAL);
        eventService.saveEvent(event);
        redirectAttributes.addFlashAttribute("successMessage", "Event created successfully!");
        return "redirect:/event/list";
    }

    // Φόρμα επεξεργασίας event (μόνο οργανισμός που ανήκει ή admin)
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Event event = eventService.getEvent(id);
        if (event == null) {
            return "redirect:/event/list";
        }
        model.addAttribute("event", event);
        model.addAttribute("organizations", organizationService.getApprovedOrganizations());
        model.addAttribute("activePage", "events");
        return "event/event-form";
    }

    // Υποβολή αλλαγών σε event
    @PostMapping("/edit/{id}")
    public String updateEvent(@PathVariable Long id,
                              @ModelAttribute("event") Event event,
                              RedirectAttributes redirectAttributes) {
        if (event.getDateTime() != null && event.getDateTime().isBefore(java.time.LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", "The event date cannot be in the past.");
            return "redirect:/event/edit/" + id;
        }
        eventService.updateEvent(id, event);
        redirectAttributes.addFlashAttribute("successMessage", "Event updated and submitted for approval.");
        return "redirect:/event/list";
    }

    // Ακύρωση event (οργανισμός που ανήκει ή admin)
    @PostMapping("/cancel/{id}")
    public String cancelEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        eventService.cancelEvent(id);
        redirectAttributes.addFlashAttribute("successMessage", "Event cancelled successfully.");
        return "redirect:/event/list";
    }

    // Διαγραφή event (οργανισμός που ανήκει ή admin)
    @PostMapping("/delete/{id}")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        eventService.deleteEvent(id);
        redirectAttributes.addFlashAttribute("successMessage", "Event deleted successfully.");
        return "redirect:/event/list";
    }
}
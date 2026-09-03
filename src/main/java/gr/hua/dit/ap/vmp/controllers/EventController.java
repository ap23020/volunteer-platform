package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.Event;
import gr.hua.dit.ap.vmp.entities.EventStatus;
import gr.hua.dit.ap.vmp.service.EventService;
import gr.hua.dit.ap.vmp.service.OrganizationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/event")
public class EventController {

    private final EventService eventService;
    private final OrganizationService organizationService; // για να φέρουμε οργανισμούς στο dropdown

    public EventController(EventService eventService, OrganizationService organizationService) {
        this.eventService = eventService;
        this.organizationService = organizationService;
    }

    // Λίστα όλων των events
    @GetMapping("/list")
    public String listEvents(Model model) {
        model.addAttribute("events", eventService.getEvents());
        model.addAttribute("activePage", "events");
        return "event/events";
    }

    // Φόρμα δημιουργίας νέου event
    @GetMapping("/new")
    public String showEventForm(Model model) {
        model.addAttribute("event", new Event());
        model.addAttribute("organizations", organizationService.getOrganizations());
        model.addAttribute("activePage", "events");
        return "event/event-form";
    }

    // Υποβολή νέου event
    @PostMapping("/new")
    public String createEvent(@ModelAttribute("event") Event event,
                              RedirectAttributes redirectAttributes) {
        event.setStatus(EventStatus.PENDING_APPROVAL); // προεπιλογή, θα αλλάξει με approval flow
        eventService.saveEvent(event);
        redirectAttributes.addFlashAttribute("successMessage", "Event created successfully!");
        return "redirect:/event/list";
    }

    // Φόρμα επεξεργασίας event (π.χ. απορριφθέν)
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Event event = eventService.getEvent(id);
        if (event == null) {
            return "redirect:/event/list";
        }
        model.addAttribute("event", event);
        model.addAttribute("organizations", organizationService.getOrganizations());
        model.addAttribute("activePage", "events");
        return "event/event-form";
    }

    // Υποβολή αλλαγών σε event
    @PostMapping("/edit/{id}")
    public String updateEvent(@PathVariable Long id,
                              @ModelAttribute("event") Event event,
                              RedirectAttributes redirectAttributes) {
        eventService.updateEvent(id, event);
        redirectAttributes.addFlashAttribute("successMessage", "Event updated and submitted for approval.");
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

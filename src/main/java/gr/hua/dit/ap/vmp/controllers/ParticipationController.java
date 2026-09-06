package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.Event;
import gr.hua.dit.ap.vmp.entities.Participation;
import gr.hua.dit.ap.vmp.entities.ParticipationStatus;
import gr.hua.dit.ap.vmp.entities.Volunteer;
import gr.hua.dit.ap.vmp.service.EventService;
import gr.hua.dit.ap.vmp.service.ParticipationService;
import gr.hua.dit.ap.vmp.service.VolunteerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/participation")
public class ParticipationController {

    private final ParticipationService participationService;
    private final VolunteerService volunteerService;
    private final EventService eventService;

    public ParticipationController(ParticipationService participationService,
                                   VolunteerService volunteerService,
                                   EventService eventService) {
        this.participationService = participationService;
        this.volunteerService = volunteerService;
        this.eventService = eventService;
    }

    // Εμφάνιση φόρμας δήλωσης συμμετοχής
    @GetMapping("/new")
    public String showParticipationForm(@RequestParam Long eventId, Model model) {
        Event selectedEvent = eventService.getEvent(eventId);
        if (selectedEvent == null) {
            return "redirect:/event/list";
        }

        // Λήψη τρέχοντος συνδεδεμένου χρήστη
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        boolean isVolunteer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VOLUNTEER"));

        if (isVolunteer) {
            // Εθελοντής: φορτώνουμε τον εαυτό του, δεν περνάμε λίστα
            Volunteer currentVolunteer = volunteerService.getVolunteerByEmail(email);
            model.addAttribute("currentVolunteer", currentVolunteer);
        } else {
            // Για admin ή άλλους ρόλους, παρέχουμε λίστα εθελοντών (π.χ. για testing)
            model.addAttribute("volunteers", volunteerService.getVolunteers());
        }

        model.addAttribute("selectedEvent", selectedEvent);
        model.addAttribute("activePage", "participation");
        return "participation/participation-form";
    }

    // Υποβολή δήλωσης συμμετοχής
    @PostMapping("/new")
    public String submitParticipation(@RequestParam Long eventId,
                                      @RequestParam(required = false) Long volunteerId,
                                      RedirectAttributes redirectAttributes) {
        // Αν ο χρήστης είναι εθελοντής, χρησιμοποιούμε αυτόματα τον τρέχοντα εθελοντή
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isVolunteer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VOLUNTEER"));

        if (isVolunteer) {
            String email = auth.getName();
            Volunteer currentVolunteer = volunteerService.getVolunteerByEmail(email);
            if (currentVolunteer != null) {
                volunteerId = currentVolunteer.getId();
            }
        }

        if (volunteerId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Volunteer not found or not authorized.");
            return "redirect:/participation/new?eventId=" + eventId;
        }

        String error = participationService.createParticipation(volunteerId, eventId);
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
            return "redirect:/participation/new?eventId=" + eventId;
        }

        redirectAttributes.addFlashAttribute("successMessage", "Application submitted successfully!");
        return "redirect:/participation/list";
    }

    // Λίστα όλων των συμμετοχών (με φίλτρα)
    @GetMapping("/list")
    public String listParticipations(@RequestParam(required = false) Long eventId,
                                     @RequestParam(required = false) ParticipationStatus status,
                                     Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isVolunteer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VOLUNTEER"));

        if (isVolunteer) {
            String email = auth.getName();
            Volunteer currentVolunteer = volunteerService.getVolunteerByEmail(email);
            if (currentVolunteer != null) {
                return "redirect:/participation/volunteer/" + currentVolunteer.getId();
            }
        }

        // Για οργανισμούς/admin
        model.addAttribute("participations", participationService.getFilteredParticipations(eventId, status));
        model.addAttribute("events", eventService.getEvents());
        model.addAttribute("statuses", ParticipationStatus.values());
        model.addAttribute("selectedEventId", eventId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activePage", "participation");
        return "participation/participations";
    }

    // Έγκριση συμμετοχής (μόνο ORGANIZATION ή ADMIN)
    @PostMapping("/approve/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZATION', 'ADMIN')")
    public String approveParticipation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        participationService.approveParticipation(id);
        redirectAttributes.addFlashAttribute("successMessage", "Participation approved.");
        return "redirect:/participation/list";
    }

    // Απόρριψη συμμετοχής (μόνο ORGANIZATION ή ADMIN)
    @PostMapping("/reject/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZATION', 'ADMIN')")
    public String rejectParticipation(@PathVariable Long id,
                                      @RequestParam(required = false) String reason,
                                      RedirectAttributes redirectAttributes) {
        participationService.rejectParticipation(id, reason);
        redirectAttributes.addFlashAttribute("successMessage", "Participation rejected.");
        return "redirect:/participation/list";
    }

    // Check-in εθελοντή (μόνο ORGANIZATION ή ADMIN)
    @PostMapping("/checkin/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZATION', 'ADMIN')")
    public String checkInVolunteer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        participationService.checkInVolunteer(id);
        redirectAttributes.addFlashAttribute("successMessage", "Check-in successful.");
        return "redirect:/participation/list";
    }

    // Λίστα συμμετοχών ανά εκδήλωση (για τον οργανισμό)
    @GetMapping("/event/{eventId}")
    public String listParticipationsByEvent(@PathVariable Long eventId, Model model) {
        model.addAttribute("participations", participationService.getParticipationsByEvent(eventId));
        model.addAttribute("event", eventService.getEvent(eventId));
        model.addAttribute("activePage", "participation");
        return "participation/event-participations";
    }

    // Λίστα συμμετοχών ανά εθελοντή (για τον εθελοντή)
    @GetMapping("/volunteer/{volunteerId}")
    public String listParticipationsByVolunteer(@PathVariable Long volunteerId, Model model) {
        model.addAttribute("participations", participationService.getActiveOrValidParticipationsByVolunteer(volunteerId));
        model.addAttribute("volunteer", volunteerService.getVolunteer(volunteerId));
        model.addAttribute("activePage", "participation");
        return "participation/volunteer-participations";
    }

    // Ακύρωση συμμετοχής
    @PostMapping("/cancel/{id}")
    public String cancelParticipation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Participation participation = participationService.getParticipation(id);
        if (participation == null || participation.getVolunteer() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Participation not found.");
            return "redirect:/participation/list";
        }

        Long volunteerId = participation.getVolunteer().getId();
        participationService.cancelParticipation(id);
        redirectAttributes.addFlashAttribute("successMessage", "Participation cancelled successfully.");
        return "redirect:/participation/volunteer/" + volunteerId;
    }
}
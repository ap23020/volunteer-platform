package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.Event;
import gr.hua.dit.ap.vmp.entities.Participation;
import gr.hua.dit.ap.vmp.entities.ParticipationStatus;
import gr.hua.dit.ap.vmp.entities.Volunteer;
import gr.hua.dit.ap.vmp.service.ParticipationService;
import gr.hua.dit.ap.vmp.service.VolunteerService;
import gr.hua.dit.ap.vmp.service.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
    public String showParticipationForm(@RequestParam(required = false) Long eventId, Model model) {
        model.addAttribute("participation", new Participation());
        model.addAttribute("volunteers", volunteerService.getVolunteers());
        model.addAttribute("events", eventService.getApprovedEvents());   // <-- μόνο APPROVED
        if (eventId != null) {
            model.addAttribute("selectedEventId", eventId);
        }
        model.addAttribute("activePage", "participation");
        return "participation/participation-form";
    }

    // Υποβολή δήλωσης
    @PostMapping("/new")
    public String submitParticipation(@RequestParam Long volunteerId,
                                      @RequestParam Long eventId,
                                      RedirectAttributes redirectAttributes) {
        String error = participationService.createParticipation(volunteerId, eventId);
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
            return "redirect:/participation/new";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Application submitted successfully!");
        return "redirect:/participation/list";
    }

    // Λίστα όλων των συμμετοχών (προσωρινά γενική)
    @GetMapping("/list")
    public String listParticipations(@RequestParam(required = false) Long eventId,
                                     @RequestParam(required = false) ParticipationStatus status,
                                     Model model) {
        model.addAttribute("participations", participationService.getFilteredParticipations(eventId, status));
        model.addAttribute("events", eventService.getEvents());
        model.addAttribute("statuses", ParticipationStatus.values());
        model.addAttribute("selectedEventId", eventId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activePage", "participation");
        return "participation/participations";
    }

    // Έγκριση/απόρριψη (POST)
    @PostMapping("/approve/{id}")
    public String approveParticipation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        participationService.approveParticipation(id);
        redirectAttributes.addFlashAttribute("successMessage", "Participation approved.");
        return "redirect:/participation/list";
    }

    @PostMapping("/reject/{id}")
    public String rejectParticipation(@PathVariable Long id,
                                      @RequestParam(required = false) String reason,
                                      RedirectAttributes redirectAttributes) {
        participationService.rejectParticipation(id, reason);
        redirectAttributes.addFlashAttribute("successMessage", "Participation rejected.");
        return "redirect:/participation/list";
    }

    // Check-in
    @PostMapping("/checkin/{id}")
    public String checkInVolunteer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        participationService.checkInVolunteer(id);
        redirectAttributes.addFlashAttribute("successMessage", "Check-in successful.");
        return "redirect:/participation/list";
    }

    // Λίστα συμμετοχών ανά εκδήλωση (για τον οργανισμό)
    @GetMapping("/event/{eventId}")
    public String listParticipationsByEvent(@PathVariable Long eventId, Model model) {
        model.addAttribute("participations", participationService.getParticipationsByEvent(eventId));
        model.addAttribute("event", eventService.getEvent(eventId)); // για να δείξουμε τίτλο
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
        participationService.cancelParticipation(id);
        redirectAttributes.addFlashAttribute("successMessage", "Participation cancelled successfully.");
        return "redirect:/participation/list";
    }
}

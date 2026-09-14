package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.*;
import gr.hua.dit.ap.vmp.service.EventService;
import gr.hua.dit.ap.vmp.service.ParticipationService;
import gr.hua.dit.ap.vmp.service.UserService;
import gr.hua.dit.ap.vmp.service.VolunteerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import gr.hua.dit.ap.vmp.util.CsvExporter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Controller
@RequestMapping("/participation")
public class ParticipationController {

    private final ParticipationService participationService;
    private final VolunteerService volunteerService;
    private final EventService eventService;
    private final UserService userService;

    public ParticipationController(ParticipationService participationService,
                                   VolunteerService volunteerService,
                                   EventService eventService,
                                   UserService userService) {
        this.participationService = participationService;
        this.volunteerService = volunteerService;
        this.eventService = eventService;
        this.userService = userService;
    }

    // Εμφάνιση φόρμας δήλωσης συμμετοχής
    // FIX: Μόνο VOLUNTEER ή ADMIN μπορούν να φτάσουν εδώ (βλ. SecurityConfig)
    @GetMapping("/new")
    public String showParticipationForm(@RequestParam Long eventId, Model model,
                                        RedirectAttributes redirectAttributes) {
        Event selectedEvent = eventService.getEvent(eventId);
        if (selectedEvent == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Event not found.");
            return "redirect:/event/list";
        }

        // Έλεγχος ότι η δράση είναι εγκεκριμένη
        if (selectedEvent.getStatus() != EventStatus.APPROVED) {
            redirectAttributes.addFlashAttribute("errorMessage", "You can only apply to approved events.");
            return "redirect:/event/list";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        boolean isVolunteer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VOLUNTEER"));

        if (isVolunteer) {
            Volunteer currentVolunteer = volunteerService.getVolunteerByEmail(email);
            if (currentVolunteer == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Volunteer profile not found.");
                return "redirect:/event/list";
            }
            model.addAttribute("currentVolunteer", currentVolunteer);
        } else {
            // Admin: δυνατότητα επιλογής εθελοντή
            model.addAttribute("volunteers", volunteerService.getVolunteers());
        }

        model.addAttribute("selectedEvent", selectedEvent);
        model.addAttribute("activePage", "participation");
        return "participation/participation-form";
    }

    // Υποβολή δήλωσης συμμετοχής
    // FIX: Για VOLUNTEER, το volunteerId προέρχεται ΠΑΝΤΑ από το session (αγνοείται τυχόν input)
    @PostMapping("/new")
    public String submitParticipation(@RequestParam Long eventId,
                                      @RequestParam(required = false) Long volunteerId,
                                      RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        boolean isVolunteer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VOLUNTEER"));

        // Για εθελοντή: το volunteerId προέρχεται από το session (αγνοούμε τυχόν δικό του)
        if (isVolunteer) {
            Volunteer currentVolunteer = volunteerService.getVolunteerByEmail(email);
            if (currentVolunteer == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Volunteer profile not found.");
                return "redirect:/event/list";
            }
            volunteerId = currentVolunteer.getId();
        }

        // Για admin: απαιτείται volunteerId
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

    // Λίστα συμμετοχών με φίλτρα – διαφοροποιείται ανά ρόλο
    @GetMapping("/list")
    public String listParticipations(@RequestParam(required = false) Long eventId,
                                     @RequestParam(required = false) ParticipationStatus status,
                                     Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        boolean isVolunteer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VOLUNTEER"));
        boolean isOrganization = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZATION"));

        // Ο εθελοντής οδηγείται στη δική του σελίδα
        if (isVolunteer) {
            Volunteer currentVolunteer = volunteerService.getVolunteerByEmail(email);
            if (currentVolunteer != null) {
                return "redirect:/participation/volunteer/" + currentVolunteer.getId();
            }
        }

        List<Participation> participations;

        if (isOrganization) {
            User user = userService.findByEmail(email);
            if (user instanceof OrganizationUser) {
                OrganizationUser orgUser = (OrganizationUser) user;
                Long orgId = orgUser.getOrganization().getId();

                participations = participationService.getParticipationsByOrganization(orgId);

                if (eventId != null) {
                    participations = participations.stream()
                            .filter(p -> p.getEvent().getId().equals(eventId))
                            .toList();
                }
                if (status != null) {
                    participations = participations.stream()
                            .filter(p -> p.getStatus() == status)
                            .toList();
                }

                // Μόνο τα events του οργανισμού στο dropdown
                model.addAttribute("events", eventService.getEventsByOrganization(orgId));
            } else {
                participations = List.of();
                model.addAttribute("events", List.of());
            }
        } else {
            // Admin: όλα
            participations = participationService.getFilteredParticipations(eventId, status);
            model.addAttribute("events", eventService.getEvents());
        }

        model.addAttribute("participations", participations);
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
    public String listParticipationsByEvent(@PathVariable Long eventId, Model model,
                                            RedirectAttributes redirectAttributes) {
        Event event = eventService.getEvent(eventId);
        if (event == null) {
            return "redirect:/participation/list";
        }

        // Έλεγχος ιδιοκτησίας για οργανισμούς
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isOrganization = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZATION"));

        if (isOrganization) {
            User user = userService.findByEmail(auth.getName());
            if (user instanceof OrganizationUser) {
                OrganizationUser orgUser = (OrganizationUser) user;
                if (event.getOrganization() == null
                        || !event.getOrganization().getId().equals(orgUser.getOrganization().getId())) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "You can only view participations for your own events.");
                    return "redirect:/participation/list";
                }
            }
        }

        model.addAttribute("participations", participationService.getParticipationsByEvent(eventId));
        model.addAttribute("event", event);
        model.addAttribute("activePage", "participation");
        return "participation/event-participations";
    }

    // Λίστα συμμετοχών ανά εθελοντή (για τον εθελοντή)
    // FIX: Έλεγχος ότι ο εθελοντής βλέπει ΜΟΝΟ τις δικές του συμμετοχές
    @GetMapping("/volunteer/{volunteerId}")
    public String listParticipationsByVolunteer(@PathVariable Long volunteerId, Model model,
                                                RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Έλεγχος ιδιοκτησίας για εθελοντή
        if (!isAdmin) {
            Volunteer currentVolunteer = volunteerService.getVolunteerByEmail(email);
            if (currentVolunteer == null || !currentVolunteer.getId().equals(volunteerId)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "You can only view your own participations.");
                return "redirect:/participation/list";
            }
        }

        model.addAttribute("participations",
                participationService.getActiveOrValidParticipationsByVolunteer(volunteerId));
        model.addAttribute("volunteer", volunteerService.getVolunteer(volunteerId));
        model.addAttribute("activePage", "participation");
        return "participation/volunteer-participations";
    }

    // Ακύρωση συμμετοχής
    // FIX: Έλεγχος ότι ο συνδεδεμένος χρήστης είναι ο ιδιοκτήτης (ή admin)
    @PostMapping("/cancel/{id}")
    public String cancelParticipation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Participation participation = participationService.getParticipation(id);
        if (participation == null || participation.getVolunteer() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Participation not found.");
            return "redirect:/participation/list";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Έλεγχος ιδιοκτησίας για εθελοντή
        if (!isAdmin) {
            Volunteer currentVolunteer = volunteerService.getVolunteerByEmail(email);
            if (currentVolunteer == null
                    || !currentVolunteer.getId().equals(participation.getVolunteer().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "You cannot cancel someone else's participation.");
                return "redirect:/participation/list";
            }
        }

        Long volunteerId = participation.getVolunteer().getId();
        participationService.cancelParticipation(id);
        redirectAttributes.addFlashAttribute("successMessage", "Participation cancelled successfully.");
        return "redirect:/participation/volunteer/" + volunteerId;
    }

    // Εξαγωγή CSV
    // FIX: Μόνο ORGANIZATION ή ADMIN (ο volunteer δεν έχει πρόσβαση)
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ORGANIZATION', 'ADMIN')")
    public ResponseEntity<byte[]> exportParticipations(@RequestParam(required = false) Long eventId,
                                                       @RequestParam(required = false) ParticipationStatus status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        boolean isOrganization = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZATION"));

        List<Participation> participations;

        if (isOrganization) {
            User user = userService.findByEmail(email);
            if (user instanceof OrganizationUser) {
                OrganizationUser orgUser = (OrganizationUser) user;
                participations = participationService.getParticipationsByOrganization(orgUser.getOrganization().getId());
            } else {
                participations = List.of();
            }
        } else {
            participations = participationService.getFilteredParticipations(eventId, status);
        }

        List<String> headers = List.of("ID", "Volunteer", "Event", "Status", "Submitted At");
        List<List<String>> rows = participations.stream()
                .map(p -> List.of(
                        String.valueOf(p.getId()),
                        p.getVolunteer().getFirstName() + " " + p.getVolunteer().getLastName(),
                        p.getEvent().getTitle(),
                        p.getStatus().name(),
                        p.getCreatedAt() != null ? p.getCreatedAt().toString() : ""
                ))
                .toList();

        String csv = CsvExporter.toCsv(headers, rows);
        byte[] bytes = csv.getBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=participations.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }
}
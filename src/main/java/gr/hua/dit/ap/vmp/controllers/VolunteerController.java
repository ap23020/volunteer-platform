package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.Role;
import gr.hua.dit.ap.vmp.entities.UserStatus;
import gr.hua.dit.ap.vmp.entities.Volunteer;
import gr.hua.dit.ap.vmp.service.UserService;
import gr.hua.dit.ap.vmp.service.VolunteerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;

@Controller
@RequestMapping("/volunteer")
public class VolunteerController {

    private final VolunteerService volunteerService;
    private final UserService userService;

    public VolunteerController(VolunteerService volunteerService,
                               UserService userService) {
        this.volunteerService = volunteerService;
        this.userService = userService;
    }

    // Εμφάνιση φόρμας εγγραφής εθελοντή
    @GetMapping("/register")
    public String showVolunteerRegistrationForm(Model model) {
        model.addAttribute("volunteer", new Volunteer());
        model.addAttribute("activePage", "register");
        return "volunteer/register-volunteer";   // <-- αν το αρχείο σου λέγεται έτσι
    }

    // Υποβολή φόρμας εγγραφής
    @PostMapping("/register")
    public String registerVolunteer(@ModelAttribute("volunteer") Volunteer volunteer,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {

        // Έλεγχος ύπαρξης email
        if (userService.isEmailTaken(volunteer.getEmail())) {
            model.addAttribute("errorMessage", "A user with this email already exists.");
            model.addAttribute("volunteer", volunteer);
            model.addAttribute("activePage", "register");
            return "volunteer/register-volunteer";   // <-- ίδιο εδώ
        }

        volunteer.setRole(Role.VOLUNTEER);
        volunteer.setStatus(UserStatus.PENDING_APPROVAL);
        volunteerService.saveVolunteer(volunteer);

        redirectAttributes.addFlashAttribute("successMessage", "Registration submitted! Awaiting approval.");
        return "redirect:/registration-pending";
    }

    // Λίστα ενεργών εθελοντών
    @GetMapping("/list")
    public String listVolunteers(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 Model model) {
        Page<Volunteer> volunteerPage = volunteerService.getVolunteersPaginated(page, size);

        model.addAttribute("volunteerPage", volunteerPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", volunteerPage.getTotalPages());
        model.addAttribute("totalItems", volunteerPage.getTotalElements());
        model.addAttribute("activePage", "volunteers");
        return "volunteer/volunteers";
    }

    // Διαγραφή εθελοντή
    @PostMapping("/delete/{id}")
    public String deleteVolunteer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        volunteerService.deleteVolunteer(id);
        redirectAttributes.addFlashAttribute("successMessage", "Volunteer deleted successfully.");
        return "redirect:/volunteer/list";
    }

    // Προβολή συμμετοχών εθελοντή (αν τη χρειάζεσαι)
    @GetMapping("/{id}/participations")
    public String viewParticipations(@PathVariable Long id, Model model) {
        model.addAttribute("volunteer", volunteerService.getVolunteer(id));
        // Εδώ μπορείς να φορτώσεις τις συμμετοχές αν έχεις service
        // model.addAttribute("participations", participationService.getParticipationsByVolunteer(id));
        model.addAttribute("activePage", "volunteers");
        return "volunteer/volunteer-participations";
    }
}

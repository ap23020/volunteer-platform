package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.Role;
import gr.hua.dit.ap.vmp.entities.UserStatus;
import gr.hua.dit.ap.vmp.entities.Volunteer;
import gr.hua.dit.ap.vmp.service.UserService;
import gr.hua.dit.ap.vmp.service.VolunteerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/volunteer")
public class VolunteerController {

    private final VolunteerService volunteerService;
    private final UserService userService; // <-- Δήλωση πεδίου

    public VolunteerController(VolunteerService volunteerService,
                               UserService userService) { // <-- Εισαγωγή στον constructor
        this.volunteerService = volunteerService;
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showVolunteerRegistrationForm(Model model) {
        model.addAttribute("volunteer", new Volunteer());
        model.addAttribute("activePage", "register");
        return "volunteer/register-volunteer";
    }

    @PostMapping("/register")
    public String registerVolunteer(@ModelAttribute("volunteer") Volunteer volunteer,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {

        // Έλεγχος ύπαρξης email
        if (userService.isEmailTaken(volunteer.getEmail())) {
            model.addAttribute("errorMessage", "A user with this email already exists.");
            model.addAttribute("volunteer", volunteer);
            model.addAttribute("activePage", "register");
            return "volunteer/register";
        }

        volunteer.setRole(Role.VOLUNTEER);
        volunteer.setStatus(UserStatus.PENDING_APPROVAL);
        volunteerService.saveVolunteer(volunteer);
        redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Awaiting approval.");
        return "redirect:/volunteer/list";
    }

    @GetMapping("/list")
    public String listVolunteers(Model model) {
        model.addAttribute("volunteers", volunteerService.getVolunteers());
        model.addAttribute("activePage", "volunteers");
        return "volunteer/volunteers";
    }

    @PostMapping("/delete/{id}")
    public String deleteVolunteer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        volunteerService.deleteVolunteer(id);
        redirectAttributes.addFlashAttribute("successMessage", "Volunteer deleted successfully.");
        return "redirect:/volunteer/list";
    }
}

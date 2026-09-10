package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.*;
import gr.hua.dit.ap.vmp.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    // Εμφάνιση φόρμας προφίλ
    @GetMapping
    public String showProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName());

        if (currentUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("activePage", "profile");
        return "profile/profile";
    }

    // Αποθήκευση αλλαγών προφίλ
    @PostMapping
    public String updateProfile(@RequestParam(required = false) String phone,
                                @RequestParam(required = false) String firstName,
                                @RequestParam(required = false) String lastName,
                                @RequestParam(required = false) String skills,
                                @RequestParam(required = false) String interests,
                                RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName());

        if (currentUser == null) {
            return "redirect:/login";
        }

        currentUser.setPhone(phone);

        if (currentUser instanceof Volunteer volunteer) {
            volunteer.setFirstName(firstName);
            volunteer.setLastName(lastName);
            volunteer.setSkills(skills);
            volunteer.setInterests(interests);
        } else if (currentUser instanceof OrganizationUser orgUser) {
            orgUser.setFirstName(firstName);
            orgUser.setLastName(lastName);
        } else if (currentUser instanceof Admin admin) {
            admin.setFirstName(firstName);
            admin.setLastName(lastName);
        }

        userService.saveUser(currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        return "redirect:/profile";
    }
}
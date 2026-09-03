package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.OrganizationUser;
import gr.hua.dit.ap.vmp.entities.Role;
import gr.hua.dit.ap.vmp.entities.UserStatus;
import gr.hua.dit.ap.vmp.service.OrganizationService;
import gr.hua.dit.ap.vmp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/organization")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserService userService;   // <-- δήλωση πεδίου

    public OrganizationController(OrganizationService organizationService,
                                  UserService userService) { // <-- εισαγωγή στον constructor
        this.organizationService = organizationService;
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showOrganizationRegistrationForm(Model model) {
        model.addAttribute("organizationUser", new OrganizationUser());
        model.addAttribute("activePage", "register");
        return "organization/register-org";
    }

    @PostMapping("/register")
    public String registerOrganization(@ModelAttribute("organizationUser") OrganizationUser organizationUser,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {

        // Έλεγχος ύπαρξης email
        if (userService.isEmailTaken(organizationUser.getEmail())) {
            model.addAttribute("errorMessage", "A user with this email already exists.");
            model.addAttribute("organizationUser", organizationUser);
            model.addAttribute("activePage", "register");
            return "organization/register-org";
        }

        organizationUser.setRole(Role.ORGANIZATION);
        organizationUser.setStatus(UserStatus.PENDING_APPROVAL);
        organizationService.saveOrganizationUser(organizationUser);
        redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Awaiting approval.");
        return "redirect:/organization/list";
    }

    @GetMapping("/list")
    public String listOrganizations(Model model) {
        model.addAttribute("organizations", organizationService.getOrganizations());
        model.addAttribute("activePage", "organizations");
        return "organization/organizations";
    }

    @GetMapping("/users")
    public String listOrganizationUsers(Model model) {
        model.addAttribute("organizationUsers", organizationService.getOrganizationUsers());
        model.addAttribute("activePage", "organizationUsers");
        return "organization/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteOrganizationUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        organizationService.deleteOrganizationUser(id);
        redirectAttributes.addFlashAttribute("successMessage", "Organization user deleted successfully.");
        return "redirect:/organization/users";
    }

    @PostMapping("/delete/{id}")
    public String deleteOrganization(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        organizationService.deleteOrganization(id);
        redirectAttributes.addFlashAttribute("successMessage", "Organization deleted successfully.");
        return "redirect:/organization/list";
    }
}

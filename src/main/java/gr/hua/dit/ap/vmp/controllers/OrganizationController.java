package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.Organization;
import gr.hua.dit.ap.vmp.entities.OrganizationUser;
import gr.hua.dit.ap.vmp.entities.Role;
import gr.hua.dit.ap.vmp.entities.UserStatus;
import gr.hua.dit.ap.vmp.service.OrganizationService;
import gr.hua.dit.ap.vmp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/organization")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserService userService;

    public OrganizationController(OrganizationService organizationService,
                                  UserService userService) {
        this.organizationService = organizationService;
        this.userService = userService;
    }

    // ===== Οργανισμός =====
    @GetMapping("/register")
    public String showOrganizationRegistrationForm(Model model) {
        model.addAttribute("organization", new Organization());
        model.addAttribute("activePage", "register");
        return "organization/organization-register";
    }

    @PostMapping("/register")
    public String registerOrganization(@ModelAttribute("organization") Organization organization,
                                       RedirectAttributes redirectAttributes) {
        organizationService.saveOrganization(organization);
        redirectAttributes.addFlashAttribute("successMessage", "Organization registered successfully!");
        return "redirect:/organization/list";
    }

    // ===== Χρήστης Οργανισμού =====
    @GetMapping("/user/register")
    public String showOrganizationUserRegistrationForm(Model model) {
        model.addAttribute("organizationUser", new OrganizationUser());
        model.addAttribute("organizations", organizationService.getOrganizations());
        model.addAttribute("activePage", "register");
        return "organization/organization-user-register";
    }

    @PostMapping("/user/register")
    public String registerOrganizationUser(@ModelAttribute("organizationUser") OrganizationUser organizationUser,
                                           RedirectAttributes redirectAttributes,
                                           Model model) {

        // Έλεγχος email
        if (userService.isEmailTaken(organizationUser.getEmail())) {
            model.addAttribute("errorMessage", "A user with this email already exists.");
            model.addAttribute("organizations", organizationService.getOrganizations());
            model.addAttribute("activePage", "register");
            return "organization/organization-user-register";
        }

        // Φόρτωση του επιλεγμένου οργανισμού
        if (organizationUser.getOrganization() != null && organizationUser.getOrganization().getId() != null) {
            Organization org = organizationService.getOrganization(organizationUser.getOrganization().getId());
            if (org == null) {
                model.addAttribute("errorMessage", "Selected organization not found.");
                model.addAttribute("organizations", organizationService.getOrganizations());
                model.addAttribute("activePage", "register");
                return "organization/organization-user-register";
            }
            organizationUser.setOrganization(org);
        } else {
            model.addAttribute("errorMessage", "Please select an organization.");
            model.addAttribute("organizations", organizationService.getOrganizations());
            model.addAttribute("activePage", "register");
            return "organization/organization-user-register";
        }

        organizationUser.setRole(Role.ORGANIZATION);
        organizationUser.setStatus(UserStatus.PENDING_APPROVAL);
        organizationService.saveOrganizationUser(organizationUser);

        redirectAttributes.addFlashAttribute("successMessage", "Registration submitted! Awaiting admin approval.");
        return "redirect:/registration-pending";
    }

    // ===== Λίστες =====
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

    // ===== Διαγραφές =====
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

    @GetMapping("/registration-pending")
    public String showPendingPage() {
        return "registration-pending";
    }
}

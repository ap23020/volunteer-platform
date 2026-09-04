package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.NotificationType;
import gr.hua.dit.ap.vmp.entities.Organization;
import gr.hua.dit.ap.vmp.entities.OrganizationUser;
import gr.hua.dit.ap.vmp.repository.OrganizationRepository;
import gr.hua.dit.ap.vmp.repository.OrganizationUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrganizationService {

    private final OrganizationUserRepository organizationUserRepository;
    private final OrganizationRepository organizationRepository;
    private final NotificationService notificationService;

    public OrganizationService(OrganizationUserRepository organizationUserRepository,
                               OrganizationRepository organizationRepository,
                               NotificationService notificationService) {
        this.organizationUserRepository = organizationUserRepository;
        this.organizationRepository = organizationRepository;
        this.notificationService = notificationService;
    }

    // ===== Οργανισμοί =====

    @Transactional
    public List<Organization> getOrganizations() {
        return organizationRepository.findAll();
    }

    @Transactional
    public Organization getOrganization(Long id) {
        return organizationRepository.findById(id).orElse(null);
    }

    // Ελέγχει αν υπάρχει ήδη οργανισμός με αυτό το όνομα
    @Transactional
    public boolean organizationNameExists(String name) {
        return organizationRepository.findByName(name).isPresent();
    }

    // Αποθήκευση νέου οργανισμού με έλεγχο μοναδικότητας
    @Transactional
    public void saveOrganization(Organization organization) {
        if (organization.getName() == null || organization.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Organization name is required.");
        }

        // Καθαρισμός κενών προαιρετικών πεδίων
        organization.setDescription(clean(organization.getDescription()));
        organization.setWebsite(clean(organization.getWebsite()));
        organization.setPhone(clean(organization.getPhone()));

        // Έλεγχος μοναδικότητας ονόματος
        if (organizationNameExists(organization.getName())) {
            throw new IllegalArgumentException("Organization with this name already exists.");
        }

        organizationRepository.save(organization);
    }

    // ===== Χρήστες Οργανισμών =====

    @Transactional
    public List<OrganizationUser> getOrganizationUsers() {
        return organizationUserRepository.findByStatus(gr.hua.dit.ap.vmp.entities.UserStatus.ACTIVE);
    }

    @Transactional
    public OrganizationUser getOrganizationUser(Long id) {
        return organizationUserRepository.findById(id).orElse(null);
    }

    @Transactional
    public void saveOrganizationUser(OrganizationUser user) {
        // Φόρτωση πλήρους οργανισμού αν υπάρχει
        if (user.getOrganization() != null && user.getOrganization().getId() != null) {
            Organization org = getOrganization(user.getOrganization().getId());
            if (org != null) {
                user.setOrganization(org);
            }
        }

        organizationUserRepository.save(user);

        // Ειδοποίηση προς διαχειριστές
        notificationService.createNotificationForAdmins(
                NotificationType.NEW_REGISTRATION,
                "New Organization User Registration",
                "A new organization user registered with email: " + user.getEmail(),
                user,
                null
        );
    }

    @Transactional
    public void deleteOrganizationUser(Long id) {
        organizationUserRepository.deleteById(id);
    }

    @Transactional
    public void deleteOrganization(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        List<OrganizationUser> users = organizationUserRepository.findByOrganizationId(organizationId);
        organizationUserRepository.deleteAll(users);
        organizationRepository.delete(org);
    }

    // Βοηθητική μέθοδος καθαρισμού κενών strings
    private String clean(String value) {
        if (value != null && value.trim().isEmpty()) {
            return null;
        }
        return value;
    }
}

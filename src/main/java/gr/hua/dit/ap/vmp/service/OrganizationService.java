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

    // ===== Μέθοδοι για Οργανισμούς =====
    @Transactional
    public List<Organization> getOrganizations() {
        return organizationRepository.findAll();
    }

    // ===== Μέθοδοι για Χρήστες Οργανισμών =====
    @Transactional
    public List<OrganizationUser> getOrganizationUsers() {
        return organizationUserRepository.findAll();
    }

    @Transactional
    public OrganizationUser getOrganizationUser(Long id) {
        return organizationUserRepository.findById(id).orElse(null);
    }

    @Transactional
    public void saveOrganizationUser(OrganizationUser user) {
        // Έλεγχος ύπαρξης οργανισμού με το ίδιο όνομα
        Organization org = user.getOrganization();
        if (org != null && org.getName() != null) {
            Optional<Organization> existing = organizationRepository.findByName(org.getName());
            if (existing.isPresent()) {
                user.setOrganization(existing.get());
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

        // Διαγραφή των χρηστών που ανήκουν στον οργανισμό
        List<OrganizationUser> users = organizationUserRepository.findByOrganizationId(organizationId);
        organizationUserRepository.deleteAll(users);

        // Διαγραφή του οργανισμού
        organizationRepository.delete(org);
    }
}

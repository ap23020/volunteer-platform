package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.*;
import gr.hua.dit.ap.vmp.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class OrganizationService {

    private final OrganizationUserRepository organizationUserRepository;
    private final OrganizationRepository organizationRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ParticipationRepository participationRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public OrganizationService(OrganizationUserRepository organizationUserRepository,
                               OrganizationRepository organizationRepository,
                               NotificationService notificationService,
                               UserRepository userRepository,
                               EventRepository eventRepository,
                               ParticipationRepository participationRepository,
                               ReviewRepository reviewRepository,
                               NotificationRepository notificationRepository,
                               BCryptPasswordEncoder passwordEncoder) {
        this.organizationUserRepository = organizationUserRepository;
        this.organizationRepository = organizationRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.participationRepository = participationRepository;
        this.reviewRepository = reviewRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ===== Οργανισμοί =====

    @Transactional
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    @Transactional
    public List<Organization> getApprovedOrganizations() {
        return organizationRepository.findByStatus(OrganizationStatus.APPROVED);
    }

    @Transactional
    public List<Organization> getPendingOrganizations() {
        return organizationRepository.findByStatus(OrganizationStatus.PENDING_APPROVAL);
    }

    @Transactional
    public Organization getOrganization(Long id) {
        return organizationRepository.findById(id).orElse(null);
    }

    @Transactional
    public boolean organizationNameExists(String name) {
        return organizationRepository.findByName(name).isPresent();
    }

    @Transactional
    public void saveOrganization(Organization organization) {
        if (organization.getName() == null || organization.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Organization name is required.");
        }

        organization.setDescription(clean(organization.getDescription()));
        organization.setWebsite(clean(organization.getWebsite()));
        organization.setPhone(clean(organization.getPhone()));

        if (organizationNameExists(organization.getName())) {
            throw new IllegalArgumentException("Organization with this name already exists.");
        }

        organization.setStatus(OrganizationStatus.PENDING_APPROVAL);
        organizationRepository.save(organization);

        notifyAdmins(
                NotificationType.NEW_ORGANIZATION,
                "New Organization Registration",
                "A new organization \"" + organization.getName() + "\" is pending approval.",
                null,
                null
        );
    }

    @Transactional
    public void approveOrganization(Long organizationId) {
        Organization org = getOrganization(organizationId);
        if (org != null) {
            org.setStatus(OrganizationStatus.APPROVED);
            organizationRepository.save(org);

            notifyAdmins(
                    NotificationType.ORGANIZATION_APPROVED,
                    "Organization Approved",
                    "Organization \"" + org.getName() + "\" has been approved.",
                    null,
                    null
            );
        }
    }

    @Transactional
    public void rejectOrganization(Long organizationId, String reason) {
        Organization org = getOrganization(organizationId);
        if (org != null) {
            org.setStatus(OrganizationStatus.REJECTED);
            organizationRepository.save(org);

            notifyAdmins(
                    NotificationType.ORGANIZATION_REJECTED,
                    "Organization Rejected",
                    "Organization \"" + org.getName() + "\" was rejected. Reason: " + (reason != null ? reason : "N/A"),
                    null,
                    null
            );
        }
    }

    // ===== Χρήστες Οργανισμών =====

    @Transactional
    public List<OrganizationUser> getOrganizationUsers() {
        return organizationUserRepository.findByStatus(UserStatus.ACTIVE);
    }

    @Transactional
    public OrganizationUser getOrganizationUser(Long id) {
        return organizationUserRepository.findById(id).orElse(null);
    }

    @Transactional
    public void saveOrganizationUser(OrganizationUser user) {
        // Κρυπτογράφηση κωδικού πρόσβασης πριν την αποθήκευση
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Φόρτωση πλήρους οργανισμού αν υπάρχει
        if (user.getOrganization() != null && user.getOrganization().getId() != null) {
            Organization org = getOrganization(user.getOrganization().getId());
            if (org == null) {
                throw new IllegalArgumentException("Selected organization not found.");
            }
            if (org.getStatus() != OrganizationStatus.APPROVED) {
                throw new IllegalArgumentException("You can only join an approved organization.");
            }
            user.setOrganization(org);
        } else {
            throw new IllegalArgumentException("Please select an organization.");
        }

        // Έλεγχος μοναδικότητας email σε όλη την πλατφόρμα
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }

        organizationUserRepository.save(user);

        // Δημιουργία μηνύματος ειδοποίησης χωρίς email
        String fullName = (user.getFirstName() != null && user.getLastName() != null)
                ? user.getFirstName() + " " + user.getLastName()
                : user.getEmail();

        String orgName = (user.getOrganization() != null) ? user.getOrganization().getName() : "Unknown organization";

        notifyAdmins(
                NotificationType.NEW_REGISTRATION,
                "New Organization User Registration",
                "New user " + fullName + " registered for organization \"" + orgName + "\".",
                user,
                null
        );
    }

    @Transactional
    public void deleteOrganizationUser(Long id) {
        OrganizationUser user = organizationUserRepository.findById(id).orElse(null);
        if (user == null) return;

        List<Notification> notifications = notificationRepository.findByRecipientId(id);
        notificationRepository.deleteAll(notifications);

        organizationUserRepository.delete(user);
    }

    @Transactional
    public void deleteOrganization(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        List<OrganizationUser> users = organizationUserRepository.findByOrganizationId(organizationId);
        for (OrganizationUser user : users) {
            List<Notification> userNotifications = notificationRepository.findByRecipientId(user.getId());
            notificationRepository.deleteAll(userNotifications);
            organizationUserRepository.delete(user);
        }

        List<Event> events = eventRepository.findByOrganizationId(organizationId);
        for (Event event : events) {
            List<Notification> eventNotifications = notificationRepository.findByRelatedEventId(event.getId());
            notificationRepository.deleteAll(eventNotifications);

            List<Participation> participations = participationRepository.findByEventId(event.getId());
            for (Participation p : participations) {
                if (p.getReview() != null) {
                    reviewRepository.delete(p.getReview());
                }
                participationRepository.delete(p);
            }
            eventRepository.delete(event);
        }

        organizationRepository.delete(org);
    }

    // ===== Βοηθητικές μέθοδοι =====

    private void notifyAdmins(NotificationType type, String title, String message, User relatedUser, Event relatedEvent) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            notificationService.createNotification(type, title, message, admin, relatedEvent);
        }
    }

    private String clean(String value) {
        if (value != null && value.trim().isEmpty()) {
            return null;
        }
        return value;
    }
}
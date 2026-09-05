package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.*;
import gr.hua.dit.ap.vmp.repository.EventRepository;
import gr.hua.dit.ap.vmp.repository.NotificationRepository;
import gr.hua.dit.ap.vmp.repository.OrganizationRepository;
import gr.hua.dit.ap.vmp.repository.OrganizationUserRepository;
import gr.hua.dit.ap.vmp.repository.ParticipationRepository;
import gr.hua.dit.ap.vmp.repository.ReviewRepository;
import gr.hua.dit.ap.vmp.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class OrganizationService {

    // Repositories που χρησιμοποιούμε
    private final OrganizationUserRepository organizationUserRepository;
    private final OrganizationRepository organizationRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;                   // Για εύρεση admins
    private final EventRepository eventRepository;                 // Για διαγραφή events οργανισμού
    private final ParticipationRepository participationRepository; // Για διαγραφή συμμετοχών
    private final ReviewRepository reviewRepository;               // Για διαγραφή αξιολογήσεων
    private final NotificationRepository notificationRepository;   // Για διαγραφή ειδοποιήσεων

    public OrganizationService(OrganizationUserRepository organizationUserRepository,
                               OrganizationRepository organizationRepository,
                               NotificationService notificationService,
                               UserRepository userRepository,
                               EventRepository eventRepository,
                               ParticipationRepository participationRepository,
                               ReviewRepository reviewRepository,
                               NotificationRepository notificationRepository) {
        this.organizationUserRepository = organizationUserRepository;
        this.organizationRepository = organizationRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.participationRepository = participationRepository;
        this.reviewRepository = reviewRepository;
        this.notificationRepository = notificationRepository;
    }

    // ===== Οργανισμοί =====

    // Επιστρέφει ΟΛΟΥΣ τους οργανισμούς (για admin ή γενική λίστα)
    @Transactional
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    // Επιστρέφει μόνο τους ΕΓΚΕΚΡΙΜΕΝΟΥΣ οργανισμούς (για dropdowns)
    @Transactional
    public List<Organization> getApprovedOrganizations() {
        return organizationRepository.findByStatus(OrganizationStatus.APPROVED);
    }

    // Επιστρέφει οργανισμούς σε αναμονή έγκρισης (για admin)
    @Transactional
    public List<Organization> getPendingOrganizations() {
        return organizationRepository.findByStatus(OrganizationStatus.PENDING_APPROVAL);
    }

    // Βρίσκει οργανισμό με id
    @Transactional
    public Organization getOrganization(Long id) {
        return organizationRepository.findById(id).orElse(null);
    }

    // Ελέγχει αν υπάρχει οργανισμός με συγκεκριμένο όνομα
    @Transactional
    public boolean organizationNameExists(String name) {
        return organizationRepository.findByName(name).isPresent();
    }

    // Αποθήκευση νέου οργανισμού (κατά την εγγραφή)
    // Θέτουμε κατάσταση PENDING_APPROVAL και ειδοποιούμε τους admins
    @Transactional
    public void saveOrganization(Organization organization) {
        // Έλεγχος ονόματος
        if (organization.getName() == null || organization.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Organization name is required.");
        }

        // Καθαρισμός κενών προαιρετικών πεδίων
        organization.setDescription(clean(organization.getDescription()));
        organization.setWebsite(clean(organization.getWebsite()));
        organization.setPhone(clean(organization.getPhone()));

        // Μοναδικότητα ονόματος
        if (organizationNameExists(organization.getName())) {
            throw new IllegalArgumentException("Organization with this name already exists.");
        }

        // Ο νέος οργανισμός ξεκινάει σε κατάσταση αναμονής
        organization.setStatus(OrganizationStatus.PENDING_APPROVAL);
        organizationRepository.save(organization);

        // Ειδοποίηση προς τους διαχειριστές
        notifyAdmins(
                NotificationType.NEW_ORGANIZATION,
                "New Organization Registration",
                "A new organization \"" + organization.getName() + "\" is pending approval.",
                null,
                null
        );
    }

    // Έγκριση οργανισμού από admin
    @Transactional
    public void approveOrganization(Long organizationId) {
        Organization org = getOrganization(organizationId);
        if (org != null) {
            org.setStatus(OrganizationStatus.APPROVED);
            organizationRepository.save(org);

            // Ειδοποίηση προς τους admins (για ιστορικό)
            notifyAdmins(
                    NotificationType.ORGANIZATION_APPROVED,
                    "Organization Approved",
                    "Organization \"" + org.getName() + "\" has been approved.",
                    null,
                    null
            );
        }
    }

    // Απόρριψη οργανισμού από admin (προαιρετικά με αιτιολογία)
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

    // Επιστρέφει μόνο ενεργούς χρήστες οργανισμών (για δημόσια λίστα)
    @Transactional
    public List<OrganizationUser> getOrganizationUsers() {
        return organizationUserRepository.findByStatus(UserStatus.ACTIVE);
    }

    @Transactional
    public OrganizationUser getOrganizationUser(Long id) {
        return organizationUserRepository.findById(id).orElse(null);
    }

    // Αποθήκευση χρήστη οργανισμού (εγγραφή)
    @Transactional
    public void saveOrganizationUser(OrganizationUser user) {
        // Φόρτωση πλήρους οργανισμού αν υπάρχει
        if (user.getOrganization() != null && user.getOrganization().getId() != null) {
            Organization org = getOrganization(user.getOrganization().getId());
            if (org == null) {
                throw new IllegalArgumentException("Selected organization not found.");
            }
            // Επιτρέπουμε εγγραφή μόνο σε εγκεκριμένους οργανισμούς
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

        // Ειδοποίηση προς διαχειριστές
        notifyAdmins(
                NotificationType.NEW_REGISTRATION,
                "New Organization User Registration",
                "A new organization user registered with email: " + user.getEmail(),
                user,
                null
        );
    }

    // Διαγραφή χρήστη οργανισμού (καθαρίζει ειδοποιήσεις)
    @Transactional
    public void deleteOrganizationUser(Long id) {
        OrganizationUser user = organizationUserRepository.findById(id).orElse(null);
        if (user == null) return;

        // Διαγραφή ειδοποιήσεων όπου παραλήπτης είναι ο χρήστης
        List<Notification> notifications = notificationRepository.findByRecipientId(id);
        notificationRepository.deleteAll(notifications);

        // Διαγραφή του χρήστη
        organizationUserRepository.delete(user);
    }

    // Διαγραφή οργανισμού (καθαρίζει χρήστες, events, participations, reviews, notifications)
    @Transactional
    public void deleteOrganization(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        // 1. Διαγραφή χρηστών οργανισμού και των ειδοποιήσεών τους
        List<OrganizationUser> users = organizationUserRepository.findByOrganizationId(organizationId);
        for (OrganizationUser user : users) {
            List<Notification> userNotifications = notificationRepository.findByRecipientId(user.getId());
            notificationRepository.deleteAll(userNotifications);
            organizationUserRepository.delete(user);
        }

        // 2. Διαγραφή events και των εξαρτώμενων τους
        List<Event> events = eventRepository.findByOrganizationId(organizationId);
        for (Event event : events) {
            // Διαγραφή ειδοποιήσεων που αναφέρονται στο event
            List<Notification> eventNotifications = notificationRepository.findByRelatedEventId(event.getId());
            notificationRepository.deleteAll(eventNotifications);

            // Διαγραφή συμμετοχών και reviews
            List<Participation> participations = participationRepository.findByEventId(event.getId());
            for (Participation p : participations) {
                if (p.getReview() != null) {
                    reviewRepository.delete(p.getReview());
                }
                participationRepository.delete(p);
            }

            eventRepository.delete(event);
        }

        // 3. Διαγραφή ειδοποιήσεων που αναφέρονται στον ίδιο τον οργανισμό (αν υπάρχουν)
        // Στην παρούσα υλοποίηση δεν έχουμε ειδοποιήσεις απευθείας σε οργανισμό,
        // αλλά μπορεί να προστεθεί μελλοντικά.

        // 4. Διαγραφή οργανισμού
        organizationRepository.delete(org);
    }

    // ===== Βοηθητικές μέθοδοι =====

    // Ειδοποιεί όλους τους χρήστες με ρόλο ADMIN
    private void notifyAdmins(NotificationType type, String title, String message, User relatedUser, Event relatedEvent) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            notificationService.createNotification(type, title, message, admin, relatedEvent);
        }
    }

    // Μετατρέπει κενά strings σε null, ώστε να αποθηκεύονται σωστά
    private String clean(String value) {
        if (value != null && value.trim().isEmpty()) {
            return null;
        }
        return value;
    }
}
package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.*;
import gr.hua.dit.ap.vmp.repository.EventRepository;
import gr.hua.dit.ap.vmp.repository.NotificationRepository;
import gr.hua.dit.ap.vmp.repository.OrganizationUserRepository;
import gr.hua.dit.ap.vmp.repository.ParticipationRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final ParticipationRepository participationRepository;
    private final UserService userService;  // Για ανάκτηση τρέχοντος χρήστη

    public EventService(EventRepository eventRepository,
                        OrganizationUserRepository organizationUserRepository,
                        NotificationService notificationService,
                        NotificationRepository notificationRepository,
                        ParticipationRepository participationRepository,
                        UserService userService) {
        this.eventRepository = eventRepository;
        this.organizationUserRepository = organizationUserRepository;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.participationRepository = participationRepository;
        this.userService = userService;
    }

    // ===== Λίστες =====

    @Transactional
    public List<Event> getEvents() {
        return eventRepository.findAll();
    }

    @Transactional
    public List<Event> getApprovedEvents() {
        return eventRepository.findByStatus(EventStatus.APPROVED);
    }

    @Transactional
    public Event getEvent(Long id) {
        return eventRepository.findById(id).orElse(null);
    }

    // ===== Δημιουργία =====

    @Transactional
    @PreAuthorize("hasAnyRole('ORGANIZATION', 'ADMIN')")
    public void saveEvent(Event event) {
        eventRepository.save(event);

        notificationService.createNotificationForAdmins(
                NotificationType.NEW_EVENT_REQUEST,
                "New Event Request",
                "A new event \"" + event.getTitle() + "\" is pending approval.",
                null,
                event
        );
    }

    // ===== Έγκριση / Απόρριψη (admin) =====

    @Transactional
    public List<Event> getPendingEvents() {
        return eventRepository.findByStatus(EventStatus.PENDING_APPROVAL);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void approveEvent(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event != null) {
            event.setStatus(EventStatus.APPROVED);
            eventRepository.save(event);

            notifyOrganizationUsers(event, NotificationType.EVENT_APPROVED,
                    "Event Approved",
                    "Your event \"" + event.getTitle() + "\" has been approved.");
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void rejectEvent(Long eventId, String comment) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event != null) {
            event.setStatus(EventStatus.REJECTED);
            event.setAdminComment(comment);
            eventRepository.save(event);

            notifyOrganizationUsers(event, NotificationType.EVENT_REJECTED,
                    "Event Rejected",
                    "Your event \"" + event.getTitle() + "\" was rejected. Reason: " + comment);
        }
    }

    // ===== Επεξεργασία / Ακύρωση / Διαγραφή με έλεγχο ιδιοκτησίας =====

    @Transactional
    public void updateEvent(Long id, Event updatedEvent) {
        Event existing = eventRepository.findById(id).orElse(null);
        if (existing != null && canManageEvent(existing)) {
            existing.setTitle(updatedEvent.getTitle());
            existing.setDescription(updatedEvent.getDescription());
            existing.setDateTime(updatedEvent.getDateTime());
            existing.setDuration(updatedEvent.getDuration());
            existing.setLocation(updatedEvent.getLocation());
            existing.setMaxParticipants(updatedEvent.getMaxParticipants());
            existing.setCategory(updatedEvent.getCategory());
            existing.setOrganization(updatedEvent.getOrganization());
            existing.setStatus(EventStatus.PENDING_APPROVAL);
            existing.setAdminComment(null);
            eventRepository.save(existing);
        }
    }

    @Transactional
    public void cancelEvent(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event != null && event.getStatus() == EventStatus.APPROVED && canManageEvent(event)) {
            event.setStatus(EventStatus.CANCELLED);
            event.setCancelledAt(java.time.LocalDateTime.now());
            eventRepository.save(event);

            // Ακύρωση συμμετοχών
            List<Participation> participations = participationRepository.findByEventId(eventId);
            for (Participation p : participations) {
                ParticipationStatus ps = p.getStatus();
                if (ps == ParticipationStatus.PENDING_ORG_APPROVAL ||
                        ps == ParticipationStatus.APPROVED ||
                        ps == ParticipationStatus.CHECKED_IN) {
                    p.setStatus(ParticipationStatus.CANCELLED);
                    p.setCancelledAt(java.time.LocalDateTime.now());
                    participationRepository.save(p);

                    if (p.getVolunteer() != null) {
                        notificationService.createNotification(
                                NotificationType.EVENT_CANCELLED,
                                "Event Cancelled",
                                "The event \"" + event.getTitle() + "\" has been cancelled by the organizer.",
                                p.getVolunteer(),
                                event
                        );
                    }
                }
            }

            // Ειδοποίηση ενεργών οργανισμικών χρηστών
            Organization org = event.getOrganization();
            if (org != null) {
                List<OrganizationUser> orgUsers = organizationUserRepository
                        .findByOrganizationIdAndStatus(org.getId(), UserStatus.ACTIVE);
                Set<String> seenEmails = new HashSet<>();
                for (OrganizationUser orgUser : orgUsers) {
                    if (seenEmails.add(orgUser.getEmail())) {
                        notificationService.createNotification(
                                NotificationType.EVENT_CANCELLED,
                                "Event Cancelled",
                                "Your event \"" + event.getTitle() + "\" has been cancelled.",
                                orgUser,
                                event
                        );
                    }
                }
            }
        }
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id).orElse(null);
        if (event != null && canManageEvent(event)) {
            // Καθαρισμός ειδοποιήσεων
            List<Notification> notifications = notificationRepository.findByRelatedEventId(id);
            notificationRepository.deleteAll(notifications);

            // Καθαρισμός συμμετοχών
            List<Participation> participations = participationRepository.findByEventId(id);
            for (Participation p : participations) {
                if (p.getReview() != null) {
                    // Διαγραφή review αν υπάρχει σχέση
                    // (αν δεν έχεις reviewRepository εδώ, μπορείς να το αγνοήσεις)
                }
                participationRepository.delete(p);
            }

            eventRepository.delete(event);
        }
    }

    // ===== Βοηθητικές μέθοδοι =====

    private void notifyOrganizationUsers(Event event, NotificationType type, String title, String message) {
        Organization org = event.getOrganization();
        if (org != null) {
            List<OrganizationUser> orgUsers = organizationUserRepository
                    .findByOrganizationIdAndStatus(org.getId(), UserStatus.ACTIVE);
            Set<String> seenEmails = new HashSet<>();
            for (OrganizationUser orgUser : orgUsers) {
                if (seenEmails.add(orgUser.getEmail())) {
                    notificationService.createNotification(type, title, message, orgUser, event);
                }
            }
        }
    }

    private boolean canManageEvent(Event event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }

        // Ο admin μπορεί πάντα
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return true;
        }

        // Βρες τον συνδεδεμένο χρήστη
        User currentUser = userService.findByEmail(auth.getName());
        if (currentUser instanceof OrganizationUser) {
            OrganizationUser orgUser = (OrganizationUser) currentUser;
            return event.getOrganization() != null
                    && event.getOrganization().getId().equals(orgUser.getOrganization().getId());
        }
        return false;
    }
}
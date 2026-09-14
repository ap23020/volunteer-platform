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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private final UserService userService;

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

    @Transactional
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

    @Transactional
    public List<Event> getPendingEvents() {
        return eventRepository.findByStatus(EventStatus.PENDING_APPROVAL);
    }

    @Transactional
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

    // FIX: Προσθήκη ελέγχου δικαιώματος + state check
    @Transactional
    public void updateEvent(Long id, Event updatedEvent) {
        Event existing = eventRepository.findById(id).orElse(null);
        if (existing == null) {
            throw new IllegalArgumentException("Event not found.");
        }

        // FIX: Έλεγχος δικαιώματος σε επίπεδο service (defense in depth)
        if (!canManageEvent(existing)) {
            throw new AccessDeniedException("You cannot edit this event.");
        }

        // FIX: Έλεγχος αν επιτρέπεται η επεξεργασία με βάση την κατάσταση
        if (existing.getStatus() == EventStatus.APPROVED
                || existing.getStatus() == EventStatus.CANCELLED
                || existing.getStatus() == EventStatus.COMPLETED) {
            throw new IllegalStateException("Only rejected or pending events can be edited.");
        }

        existing.setTitle(updatedEvent.getTitle());
        existing.setDescription(updatedEvent.getDescription());
        existing.setDateTime(updatedEvent.getDateTime());
        existing.setDuration(updatedEvent.getDuration());
        existing.setLocation(updatedEvent.getLocation());
        existing.setMaxParticipants(updatedEvent.getMaxParticipants());
        existing.setCategory(updatedEvent.getCategory());

        // Ο οργανισμός μπορεί να αλλάξει μόνο από admin
        if (updatedEvent.getOrganization() != null) {
            existing.setOrganization(updatedEvent.getOrganization());
        }

        // Επαναφορά σε PENDING_APPROVAL μετά την επεξεργασία
        existing.setStatus(EventStatus.PENDING_APPROVAL);
        existing.setAdminComment(null);

        eventRepository.save(existing);
    }

    @Transactional
    public void cancelEvent(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            throw new IllegalArgumentException("Event not found.");
        }

        if (event.getStatus() != EventStatus.APPROVED) {
            throw new IllegalStateException("Only approved events can be cancelled.");
        }

        // FIX: Χρήση της κοινής βοηθητικής μεθόδου
        if (!canManageEvent(event)) {
            throw new AccessDeniedException("You cannot cancel this event.");
        }

        event.setStatus(EventStatus.CANCELLED);
        event.setCancelledAt(LocalDateTime.now());
        eventRepository.save(event);

        List<Participation> participations = participationRepository.findByEventId(eventId);
        for (Participation p : participations) {
            ParticipationStatus ps = p.getStatus();
            if (ps == ParticipationStatus.PENDING_ORG_APPROVAL ||
                    ps == ParticipationStatus.APPROVED ||
                    ps == ParticipationStatus.CHECKED_IN) {
                p.setStatus(ParticipationStatus.CANCELLED);
                p.setCancelledAt(LocalDateTime.now());
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

    @Transactional
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return;

        List<Notification> notifications = notificationRepository.findByRelatedEventId(id);
        notificationRepository.deleteAll(notifications);

        List<Participation> participations = participationRepository.findByEventId(id);
        for (Participation p : participations) {
            // ΣΗΜΕΙΩΣΗ: Αν έχεις ReviewRepository, μπορείς να διαγράψεις πρώτα το review εδώ
            participationRepository.delete(p);
        }

        eventRepository.delete(event);
    }

    // Επιστρέφει τα events ενός συγκεκριμένου οργανισμού
    @Transactional
    public List<Event> getEventsByOrganization(Long organizationId) {
        return eventRepository.findByOrganizationId(organizationId);
    }

    // Επιστρέφει τα events με βάση την κατάσταση
    @Transactional
    public List<Event> getEventsByStatus(EventStatus status) {
        return eventRepository.findByStatus(status);
    }

    // ============================================================
    // FIX: Νέες μέθοδοι φιλτραρίσματος ανά ρόλο
    // ============================================================

    /**
     * Φιλτράρισμα για ADMIN — βλέπει τα πάντα.
     */
    @Transactional
    public List<Event> getFilteredEventsForAdmin(Long organizationId, EventStatus status) {
        return getFilteredEventsForAdmin(organizationId, status, null, null);
    }

    /**
     * UC-05.2 / UC-05.3: Επεκτεταμένο φιλτράρισμα για ADMIN με αναζήτηση κειμένου
     * και κατηγορίας.
     */
    @Transactional
    public List<Event> getFilteredEventsForAdmin(Long organizationId, EventStatus status,
                                                 String keyword, String category) {
        List<Event> base;
        if (organizationId != null && status != null) {
            base = eventRepository.findByOrganizationIdAndStatus(organizationId, status);
        } else if (organizationId != null) {
            base = eventRepository.findByOrganizationId(organizationId);
        } else if (status != null) {
            base = eventRepository.findByStatus(status);
        } else {
            base = eventRepository.findAll();
        }
        return applySearchAndCategory(base, keyword, category);
    }

    /**
     * Φιλτράρισμα για ORGANIZATION — μόνο τα δικά του events.
     */
    @Transactional
    public List<Event> getFilteredEventsForOrganization(Long organizationId, EventStatus status) {
        return getFilteredEventsForOrganization(organizationId, status, null, null);
    }

    /**
     * UC-05.2 / UC-05.3: Επεκτεταμένο φιλτράρισμα για ORGANIZATION.
     */
    @Transactional
    public List<Event> getFilteredEventsForOrganization(Long organizationId, EventStatus status,
                                                       String keyword, String category) {
        List<Event> base = (status != null)
                ? eventRepository.findByOrganizationIdAndStatus(organizationId, status)
                : eventRepository.findByOrganizationId(organizationId);
        return applySearchAndCategory(base, keyword, category);
    }

    /**
     * Φιλτράρισμα για VOLUNTEER — μόνο εγκεκριμένα events.
     */
    @Transactional
    public List<Event> getFilteredEventsForVolunteer(Long organizationId) {
        return getFilteredEventsForVolunteer(organizationId, null, null);
    }

    /**
     * UC-05.1 / UC-05.2 / UC-05.3: Ο εθελοντής βλέπει μόνο APPROVED δράσεις, με προαιρετική
     * αναζήτηση κειμένου (τίτλος/περιγραφή/τοποθεσία) και φίλτρο κατηγορίας.
     */
    @Transactional
    public List<Event> getFilteredEventsForVolunteer(Long organizationId, String keyword, String category) {
        List<Event> base = (organizationId != null)
                ? eventRepository.findByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)
                : eventRepository.findByStatus(EventStatus.APPROVED);
        return applySearchAndCategory(base, keyword, category);
    }

    // FIX: Διατηρούμε την παλιά για backward compatibility (αν χρησιμοποιείται αλλού)
    @Deprecated
    @Transactional
    public List<Event> getFilteredEvents(Long organizationId, EventStatus status) {
        return getFilteredEventsForAdmin(organizationId, status);
    }

    // UC-05.2 / UC-05.3: κοινή εφαρμογή κριτηρίων αναζήτησης και κατηγορίας
    private List<Event> applySearchAndCategory(List<Event> events, String keyword, String category) {
        java.util.stream.Stream<Event> stream = events.stream();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String needle = keyword.trim().toLowerCase();
            stream = stream.filter(e -> containsIgnoreCase(e.getTitle(), needle)
                    || containsIgnoreCase(e.getDescription(), needle)
                    || containsIgnoreCase(e.getLocation(), needle));
        }
        if (category != null && !category.trim().isEmpty()) {
            String want = category.trim();
            stream = stream.filter(e -> e.getCategory() != null && e.getCategory().equalsIgnoreCase(want));
        }
        return stream.toList();
    }

    private boolean containsIgnoreCase(String haystack, String lowerNeedle) {
        return haystack != null && haystack.toLowerCase().contains(lowerNeedle);
    }

    /**
     * UC-05.3: διακριτές κατηγορίες που εμφανίζονται στο φίλτρο κατηγορίας.
     */
    @Transactional
    public List<String> getDistinctCategories() {
        return eventRepository.findAll().stream()
                .map(Event::getCategory)
                .filter(c -> c != null && !c.trim().isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    // ============================================================

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

    // FIX: Βοηθητική μέθοδος για έλεγχο δικαιωμάτων (χρησιμοποιείται από updateEvent & cancelEvent)
    private boolean canManageEvent(Event event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return true;

        User currentUser = userService.findByEmail(auth.getName());
        if (currentUser instanceof OrganizationUser) {
            OrganizationUser orgUser = (OrganizationUser) currentUser;
            return event.getOrganization() != null
                    && event.getOrganization().getId().equals(orgUser.getOrganization().getId());
        }
        return false;
    }
}
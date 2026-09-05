package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.*;
import gr.hua.dit.ap.vmp.repository.EventRepository;
import gr.hua.dit.ap.vmp.repository.NotificationRepository;
import gr.hua.dit.ap.vmp.repository.OrganizationUserRepository;
import gr.hua.dit.ap.vmp.repository.ParticipationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final ParticipationRepository participationRepository;

    public EventService(EventRepository eventRepository,
                        OrganizationUserRepository organizationUserRepository,
                        NotificationService notificationService,
                        NotificationRepository notificationRepository,
                        ParticipationRepository participationRepository) {
        this.eventRepository = eventRepository;
        this.organizationUserRepository = organizationUserRepository;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.participationRepository = participationRepository;
    }

    // Λίστα όλων των events
    @Transactional
    public List<Event> getEvents() {
        return eventRepository.findAll();
    }

    // Λίστα εγκεκριμένων events (για φόρμες συμμετοχής)
    @Transactional
    public List<Event> getApprovedEvents() {
        return eventRepository.findByStatus(EventStatus.APPROVED);
    }

    // Βρες event με id
    @Transactional
    public Event getEvent(Long id) {
        return eventRepository.findById(id).orElse(null);
    }

    // Δημιουργία νέου event – ειδοποιεί ΜΟΝΟ τους admins
    @Transactional
    public void saveEvent(Event event) {
        eventRepository.save(event);

        // Ειδοποίηση προς διαχειριστές (admins) για νέο event που χρειάζεται έγκριση
        notificationService.createNotificationForAdmins(
                NotificationType.NEW_EVENT_REQUEST,
                "New Event Request",
                "A new event \"" + event.getTitle() + "\" is pending approval.",
                null,
                event
        );
    }

    // Διαγραφή event με καθαρισμό εξαρτώμενων εγγραφών
    @Transactional
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id).orElse(null);
        if (event != null) {
            // Διαγραφή ειδοποιήσεων που αναφέρονται σε αυτό το event
            List<Notification> notifications = notificationRepository.findByRelatedEventId(id);
            notificationRepository.deleteAll(notifications);

            // Διαγραφή συμμετοχών (οι αξιολογήσεις θα σβηστούν λόγω cascade)
            List<Participation> participations = participationRepository.findByEventId(id);
            participationRepository.deleteAll(participations);

            // Διαγραφή του event
            eventRepository.delete(event);
        }
    }

    // Λίστα εκκρεμών events για admin
    @Transactional
    public List<Event> getPendingEvents() {
        return eventRepository.findByStatus(EventStatus.PENDING_APPROVAL);
    }

    // Έγκριση event – ειδοποιεί τον οργανισμό
    @Transactional
    public void approveEvent(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event != null) {
            event.setStatus(EventStatus.APPROVED);
            eventRepository.save(event);

            // Ειδοποίηση προς οργανισμό
            notifyOrganizationUsers(event, NotificationType.EVENT_APPROVED,
                    "Event Approved",
                    "Your event \"" + event.getTitle() + "\" has been approved.");
        }
    }

    // Απόρριψη event με σχόλιο – ειδοποιεί τον οργανισμό
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

    // Ενημέρωση (edit) event – επανυποβολή απορριφθέντος
    @Transactional
    public void updateEvent(Long id, Event updatedEvent) {
        Event existing = eventRepository.findById(id).orElse(null);
        if (existing != null) {
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

    // Ακύρωση event από οργανισμό – ακυρώνει συμμετοχές και ειδοποιεί εθελοντές + ενεργούς οργανισμούς
    @Transactional
    public void cancelEvent(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event != null && event.getStatus() == EventStatus.APPROVED) {
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

                    // Ειδοποίηση εθελοντή
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
                java.util.Set<String> seenEmails = new java.util.HashSet<>();
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

    // Βοηθητική μέθοδος: ειδοποιεί όλους τους χρήστες του οργανισμού του event
    private void notifyOrganizationUsers(Event event, NotificationType type, String title, String message) {
        Organization org = event.getOrganization();
        if (org != null) {
            List<OrganizationUser> orgUsers = organizationUserRepository
                    .findByOrganizationIdAndStatus(org.getId(), UserStatus.ACTIVE);
            java.util.Set<String> seenEmails = new java.util.HashSet<>();
            for (OrganizationUser orgUser : orgUsers) {
                if (seenEmails.add(orgUser.getEmail())) {
                    notificationService.createNotification(type, title, message, orgUser, event);
                }
            }
        }
    }
}
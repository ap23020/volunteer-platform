package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.*;
import gr.hua.dit.ap.vmp.repository.EventRepository;
import gr.hua.dit.ap.vmp.repository.OrganizationUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final NotificationService notificationService;

    // Constructor injection
    public EventService(EventRepository eventRepository,
                        OrganizationUserRepository organizationUserRepository,
                        NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.organizationUserRepository = organizationUserRepository;
        this.notificationService = notificationService;
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

    // Δημιουργία νέου event
    @Transactional
    public void saveEvent(Event event) {
        eventRepository.save(event);
    }

    // Διαγραφή event
    @Transactional
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    // Λίστα εκκρεμών events για admin
    @Transactional
    public List<Event> getPendingEvents() {
        return eventRepository.findByStatus(EventStatus.PENDING_APPROVAL);
    }

    // Έγκριση event
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

    // Απόρριψη event με σχόλιο
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

    // Ενημέρωση (edit) event – χρησιμοποιείται για επανυποβολή απορριφθέντος
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
            existing.setStatus(EventStatus.PENDING_APPROVAL); // επανυποβολή
            existing.setAdminComment(null);                   // καθαρίζουμε παλιό σχόλιο
            eventRepository.save(existing);
        }
    }

    // Βοηθητική μέθοδος: ειδοποιεί όλους τους χρήστες του οργανισμού του event
    private void notifyOrganizationUsers(Event event, NotificationType type, String title, String message) {
        Organization org = event.getOrganization();
        if (org != null) {
            List<OrganizationUser> orgUsers = organizationUserRepository.findByOrganizationId(org.getId());
            for (OrganizationUser orgUser : orgUsers) {
                notificationService.createNotification(type, title, message, orgUser, event);
            }
        }
    }
}

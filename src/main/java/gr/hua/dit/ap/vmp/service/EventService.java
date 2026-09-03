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

    public EventService(EventRepository eventRepository,
                        OrganizationUserRepository organizationUserRepository,
                        NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.organizationUserRepository = organizationUserRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public List<Event> getEvents() {
        return eventRepository.findAll();
    }

    @Transactional
    public Event getEvent(Long id) {
        return eventRepository.findById(id).orElse(null);
    }

    @Transactional
    public void saveEvent(Event event) {
        eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
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

            // Ειδοποίηση προς όλους τους χρήστες του οργανισμού
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

    // Βοηθητική μέθοδος που ειδοποιεί όλους τους OrganizationUser του event
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

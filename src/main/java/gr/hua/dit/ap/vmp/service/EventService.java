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

    @Transactional
    public void cancelEvent(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            throw new IllegalArgumentException("Event not found.");
        }

        if (event.getStatus() != EventStatus.APPROVED) {
            throw new IllegalStateException("Only approved events can be cancelled.");
        }

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
            if (p.getReview() != null) {
                // Αν έχεις reviewRepository εδώ, μπορείς να το διαγράψεις
            }
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

    // Φιλτράρισμα events με βάση οργανισμό και κατάσταση
    @Transactional
    public List<Event> getFilteredEvents(Long organizationId, EventStatus status) {
        if (organizationId != null && status != null) {
            return eventRepository.findByOrganizationIdAndStatus(organizationId, status);
        } else if (organizationId != null) {
            return eventRepository.findByOrganizationId(organizationId);
        } else if (status != null) {
            return eventRepository.findByStatus(status);
        } else {
            return eventRepository.findAll();
        }
    }

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
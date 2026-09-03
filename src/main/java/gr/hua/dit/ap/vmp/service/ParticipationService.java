package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.*;
import gr.hua.dit.ap.vmp.repository.EventRepository;
import gr.hua.dit.ap.vmp.repository.OrganizationUserRepository;
import gr.hua.dit.ap.vmp.repository.ParticipationRepository;
import gr.hua.dit.ap.vmp.repository.VolunteerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final VolunteerRepository volunteerRepository;
    private final EventRepository eventRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final NotificationService notificationService;

    public ParticipationService(ParticipationRepository participationRepository,
                                VolunteerRepository volunteerRepository,
                                EventRepository eventRepository,
                                OrganizationUserRepository organizationUserRepository,
                                NotificationService notificationService) {
        this.participationRepository = participationRepository;
        this.volunteerRepository = volunteerRepository;
        this.eventRepository = eventRepository;
        this.organizationUserRepository = organizationUserRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public List<Participation> getParticipations() {
        return participationRepository.findAll();
    }

    @Transactional
    public List<Participation> getFilteredParticipations(Long eventId, ParticipationStatus status) {
        if (eventId != null && status != null) {
            return participationRepository.findByEventIdAndStatus(eventId, status);
        } else if (eventId != null) {
            return participationRepository.findByEventId(eventId);
        } else if (status != null) {
            return participationRepository.findByStatus(status);
        } else {
            return participationRepository.findAll();
        }
    }

    @Transactional
    public List<Participation> getParticipationsByEvent(Long eventId) {
        return participationRepository.findByEventId(eventId);
    }

    @Transactional
    public List<Participation> getParticipationsByVolunteer(Long volunteerId) {
        return participationRepository.findByVolunteerId(volunteerId);
    }

    @Transactional
    public Participation getParticipation(Long id) {
        return participationRepository.findById(id).orElse(null);
    }

    @Transactional
    public String createParticipation(Long volunteerId, Long eventId) {
        Volunteer volunteer = volunteerRepository.findById(volunteerId).orElse(null);
        Event event = eventRepository.findById(eventId).orElse(null);
        if (volunteer == null || event == null) {
            return "Volunteer or Event not found.";
        }

        if (event.getStatus() != EventStatus.APPROVED) {
            return "You can only apply to approved events.";
        }

        // Έλεγχος για τυχόν ενεργή συμμετοχή στο ίδιο event
        List<Participation> existing = participationRepository.findByVolunteerIdAndEventId(volunteerId, eventId);
        boolean hasActive = existing.stream()
                .anyMatch(p -> p.getStatus() == ParticipationStatus.PENDING_ORG_APPROVAL
                        || p.getStatus() == ParticipationStatus.APPROVED
                        || p.getStatus() == ParticipationStatus.CHECKED_IN);
        if (hasActive) {
            return "You already have an active application for this event.";
        }

        int currentRegistrations = participationRepository.findByEventId(eventId).size();
        if (event.getMaxParticipants() != null && currentRegistrations >= event.getMaxParticipants()) {
            return "The event is full.";
        }

        Participation participation = new Participation(volunteer, event);
        participationRepository.save(participation);

        // Ειδοποίηση οργανισμού
        Organization org = event.getOrganization();
        if (org != null) {
            List<OrganizationUser> orgUsers = organizationUserRepository.findByOrganizationId(org.getId());
            for (OrganizationUser orgUser : orgUsers) {
                notificationService.createNotification(
                        NotificationType.NEW_REGISTRATION,
                        "New Participation Application",
                        "A volunteer applied for your event \"" + event.getTitle() + "\".",
                        orgUser,
                        event
                );
            }
        }
        return null;
    }

    @Transactional
    public void approveParticipation(Long participationId) {
        Participation participation = participationRepository.findById(participationId).orElse(null);
        if (participation != null) {
            participation.setStatus(ParticipationStatus.APPROVED);
            participation.setApprovedAt(LocalDateTime.now());
            participationRepository.save(participation);

            notificationService.createNotification(
                    NotificationType.REGISTRATION_APPROVED,
                    "Participation Approved",
                    "Your participation in " + participation.getEvent().getTitle() + " has been approved.",
                    participation.getVolunteer(),
                    participation.getEvent()
            );
        }
    }

    @Transactional
    public void rejectParticipation(Long participationId, String reason) {
        Participation participation = participationRepository.findById(participationId).orElse(null);
        if (participation != null) {
            participation.setStatus(ParticipationStatus.REJECTED);
            participation.setRejectionReason(reason);
            participationRepository.save(participation);

            notificationService.createNotification(
                    NotificationType.REGISTRATION_REJECTED,
                    "Participation Rejected",
                    "Your participation in " + participation.getEvent().getTitle() + " was rejected. Reason: " + reason,
                    participation.getVolunteer(),
                    participation.getEvent()
            );
        }
    }

    @Transactional
    public void checkInVolunteer(Long participationId) {
        Participation participation = participationRepository.findById(participationId).orElse(null);
        if (participation != null && participation.getStatus() == ParticipationStatus.APPROVED) {
            participation.setStatus(ParticipationStatus.CHECKED_IN);
            participation.setCheckInTime(LocalDateTime.now());
            participationRepository.save(participation);
        }
    }

    @Transactional
    public void cancelParticipation(Long participationId) {
        Participation participation = participationRepository.findById(participationId).orElse(null);
        if (participation != null) {
            ParticipationStatus status = participation.getStatus();
            if (status == ParticipationStatus.PENDING_ORG_APPROVAL || status == ParticipationStatus.APPROVED) {
                participation.setStatus(ParticipationStatus.CANCELLED);
                participation.setCancelledAt(LocalDateTime.now());
                participationRepository.save(participation);
            }
        }
    }

    @Transactional
    public List<Participation> getCheckinsWithoutReview() {
        return participationRepository.findAll().stream()
                .filter(p -> p.getStatus() == ParticipationStatus.CHECKED_IN)
                .filter(p -> p.getReview() == null)
                .toList();
    }

    @Transactional
    public List<Participation> getActiveOrValidParticipationsByVolunteer(Long volunteerId) {
        return participationRepository.findByVolunteerId(volunteerId).stream()
                .filter(p -> p.getStatus() != ParticipationStatus.REJECTED)
                .collect(Collectors.toList());
    }
}
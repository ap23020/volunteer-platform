package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.*;
import gr.hua.dit.ap.vmp.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;
    private final NotificationService notificationService;
    private final ParticipationRepository participationRepository;
    private final NotificationRepository notificationRepository;
    private final ReviewRepository reviewRepository;

    public VolunteerService(VolunteerRepository volunteerRepository,
                            NotificationService notificationService,
                            ParticipationRepository participationRepository,
                            NotificationRepository notificationRepository,
                            ReviewRepository reviewRepository) {
        this.volunteerRepository = volunteerRepository;
        this.notificationService = notificationService;
        this.participationRepository = participationRepository;
        this.notificationRepository = notificationRepository;
        this.reviewRepository = reviewRepository;
    }

    // ===== Λίστες & Αναζήτηση =====

    // Επιστρέφει όλους τους ενεργούς εθελοντές
    @Transactional
    public List<Volunteer> getVolunteers() {
        return volunteerRepository.findByStatus(UserStatus.ACTIVE);
    }

    // Επιστρέφει σελίδα ενεργών εθελοντών (pagination)
    @Transactional
    public Page<Volunteer> getVolunteersPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
        return volunteerRepository.findByStatus(UserStatus.ACTIVE, pageable);
    }

    @Transactional
    public Volunteer getVolunteer(Long id) {
        return volunteerRepository.findById(id).orElse(null);
    }

    // ===== Εγγραφή =====

    @Transactional
    public void saveVolunteer(Volunteer volunteer) {
        volunteerRepository.save(volunteer);

        // Ειδοποίηση προς διαχειριστές
        notificationService.createNotificationForAdmins(
                NotificationType.NEW_REGISTRATION,
                "New Volunteer Registration",
                "A new volunteer registered with email: " + volunteer.getEmail(),
                volunteer,
                null
        );
    }

    @Transactional
    public boolean isEmailTaken(String email) {
        return volunteerRepository.findByEmail(email).isPresent();
    }

    // ===== Διαγραφή =====

    @Transactional
    public void deleteVolunteer(Long id) {
        Volunteer volunteer = volunteerRepository.findById(id).orElse(null);
        if (volunteer == null) return;

        // 1. Διαγραφή συμμετοχών και των αξιολογήσεών τους
        List<Participation> participations = participationRepository.findByVolunteerId(id);
        for (Participation p : participations) {
            if (p.getReview() != null) {
                reviewRepository.delete(p.getReview());
            }
            participationRepository.delete(p);
        }

        // 2. Διαγραφή ειδοποιήσεων όπου παραλήπτης είναι ο εθελοντής
        List<Notification> notifications = notificationRepository.findByRecipientId(id);
        notificationRepository.deleteAll(notifications);

        // 3. Διαγραφή του εθελοντή
        volunteerRepository.delete(volunteer);
    }
}
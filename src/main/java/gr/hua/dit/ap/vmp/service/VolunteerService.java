package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.*;
import gr.hua.dit.ap.vmp.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private final BCryptPasswordEncoder passwordEncoder;   // <-- προσθήκη

    public VolunteerService(VolunteerRepository volunteerRepository,
                            NotificationService notificationService,
                            ParticipationRepository participationRepository,
                            NotificationRepository notificationRepository,
                            ReviewRepository reviewRepository,
                            BCryptPasswordEncoder passwordEncoder) {   // <-- προσθήκη
        this.volunteerRepository = volunteerRepository;
        this.notificationService = notificationService;
        this.participationRepository = participationRepository;
        this.notificationRepository = notificationRepository;
        this.reviewRepository = reviewRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ===== Λίστες & Αναζήτηση =====

    @Transactional
    public List<Volunteer> getVolunteers() {
        return volunteerRepository.findByStatus(UserStatus.ACTIVE);
    }

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
        // Κρυπτογράφηση κωδικού πρόσβασης πριν την αποθήκευση
        if (volunteer.getPassword() != null && !volunteer.getPassword().isEmpty()) {
            volunteer.setPassword(passwordEncoder.encode(volunteer.getPassword()));
        }

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

        List<Participation> participations = participationRepository.findByVolunteerId(id);
        for (Participation p : participations) {
            if (p.getReview() != null) {
                reviewRepository.delete(p.getReview());
            }
            participationRepository.delete(p);
        }

        List<Notification> notifications = notificationRepository.findByRecipientId(id);
        notificationRepository.deleteAll(notifications);

        volunteerRepository.delete(volunteer);
    }
}
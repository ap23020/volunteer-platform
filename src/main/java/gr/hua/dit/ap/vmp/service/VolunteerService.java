package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.NotificationType;
import gr.hua.dit.ap.vmp.entities.UserStatus;
import gr.hua.dit.ap.vmp.entities.Volunteer;
import gr.hua.dit.ap.vmp.repository.VolunteerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;
    private final NotificationService notificationService;

    public VolunteerService(VolunteerRepository volunteerRepository,
                            NotificationService notificationService) {
        this.volunteerRepository = volunteerRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public List<Volunteer> getVolunteers() {
        return volunteerRepository.findByStatus(UserStatus.ACTIVE);
    }

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
    public Volunteer getVolunteer(Long id) {
        return volunteerRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteVolunteer(Long id) {
        volunteerRepository.deleteById(id);
    }

    @Transactional
    public Page<Volunteer> getVolunteersPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
        return volunteerRepository.findByStatus(UserStatus.ACTIVE, pageable);
    }
}
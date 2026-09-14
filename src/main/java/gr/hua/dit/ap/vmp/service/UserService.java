package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.NotificationType;
import gr.hua.dit.ap.vmp.entities.OrganizationUser;
import gr.hua.dit.ap.vmp.entities.Role;
import gr.hua.dit.ap.vmp.entities.User;
import gr.hua.dit.ap.vmp.entities.UserStatus;
import gr.hua.dit.ap.vmp.entities.Volunteer;
import gr.hua.dit.ap.vmp.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final VolunteerService volunteerService;
    private final OrganizationService organizationService;

    public UserService(UserRepository userRepository,
                       NotificationService notificationService,
                       BCryptPasswordEncoder passwordEncoder,
                       @Lazy VolunteerService volunteerService,
                       @Lazy OrganizationService organizationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
        this.volunteerService = volunteerService;
        this.organizationService = organizationService;
    }

    @Transactional
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User getUser(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional
    public void saveUser(User user) {
        // Κρυπτογράφηση κωδικού πριν την αποθήκευση
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userRepository.save(user);
    }

    @Transactional
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public boolean isEmailTaken(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Transactional
    public List<User> getPendingUsers() {
        return userRepository.findByStatus(UserStatus.PENDING_APPROVAL);
    }

    @Transactional
    public void approveUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);

            notificationService.createNotification(
                    NotificationType.PROFILE_APPROVED,
                    "Account Approved",
                    "Your account has been approved.",
                    user,
                    null
            );
        }
    }

    @Transactional
    public void rejectUser(Long userId, String reason) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setStatus(UserStatus.REJECTED);
            user.setRejectionReason(reason);
            userRepository.save(user);

            notificationService.createNotification(
                    NotificationType.PROFILE_REJECTED,
                    "Account Rejected",
                    "Your account was rejected. Reason: " + reason,
                    user,
                    null
            );
        }
    }

    // UC-09.2 / FR-03: Ο διαχειριστής βλέπει όλους τους λογαριασμούς
    @Transactional
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // UC-09.2 / FR-03 / BR-10: Φυσική διαγραφή λογαριασμού εθελοντή ή χρήστη οργανισμού
    // με cascade των εξαρτημένων εγγραφών.
    @Transactional
    public String deleteUser(Long userId, String currentUserEmail) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "User not found.";
        }
        if (user.getRole() == Role.ADMIN) {
            return "Admin accounts cannot be deleted.";
        }
        if (currentUserEmail != null && currentUserEmail.equalsIgnoreCase(user.getEmail())) {
            return "You cannot delete your own account.";
        }

        if (user instanceof Volunteer) {
            volunteerService.deleteVolunteer(user.getId());
        } else if (user instanceof OrganizationUser) {
            organizationService.deleteOrganizationUser(user.getId());
        } else {
            userRepository.delete(user);
        }
        return null;
    }
}
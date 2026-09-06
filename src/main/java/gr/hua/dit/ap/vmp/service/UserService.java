package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.NotificationType;
import gr.hua.dit.ap.vmp.entities.User;
import gr.hua.dit.ap.vmp.entities.UserStatus;
import gr.hua.dit.ap.vmp.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       NotificationService notificationService,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
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
}
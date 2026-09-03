package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.NotificationType;
import gr.hua.dit.ap.vmp.entities.User;
import gr.hua.dit.ap.vmp.entities.UserStatus;
import gr.hua.dit.ap.vmp.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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
        userRepository.save(user);
    }

    @Transactional
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public boolean isEmailTaken(String email) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            UserStatus status = user.getStatus();
            if (status == UserStatus.ACTIVE || status == UserStatus.PENDING_APPROVAL) {
                return true;
            } else {
                user.setEmail(email + "_old_" + user.getId());
                userRepository.save(user);
                return false;
            }
        }
        return false;
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
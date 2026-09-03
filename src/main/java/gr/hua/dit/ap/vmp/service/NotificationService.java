package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.*;
import gr.hua.dit.ap.vmp.repository.NotificationRepository;
import gr.hua.dit.ap.vmp.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;   // <-- νέο

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<Notification> getNotifications() {
        return notificationRepository.findAll();
    }

    @Transactional
    public void createNotification(NotificationType type, String title, String message,
                                   User recipient, Event relatedEvent) {
        Notification notification = new Notification(type, title, message, recipient, relatedEvent);
        notificationRepository.save(notification);
    }

    // Νέα μέθοδος: ειδοποίηση όλων των admins
    @Transactional
    public void createNotificationForAdmins(NotificationType type, String title, String message,
                                            User relatedUser, Event relatedEvent) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            createNotification(type, title, message, admin, relatedEvent);
        }
    }
}


package gr.hua.dit.ap.vmp.repository;

import gr.hua.dit.ap.vmp.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
}

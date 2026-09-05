package gr.hua.dit.ap.vmp.repository;

import gr.hua.dit.ap.vmp.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findAll(Pageable pageable);
    List<Notification> findByRelatedEventId(Long eventId);
}

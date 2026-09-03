package gr.hua.dit.ap.vmp.repository;

import gr.hua.dit.ap.vmp.entities.Event;
import gr.hua.dit.ap.vmp.entities.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByStatus(EventStatus status);
}

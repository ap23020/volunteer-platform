package gr.hua.dit.ap.vmp.repository;

import gr.hua.dit.ap.vmp.entities.Participation;
import gr.hua.dit.ap.vmp.entities.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    List<Participation> findByEventId(Long eventId);
    List<Participation> findByVolunteerId(Long volunteerId);
    boolean existsByVolunteerIdAndEventId(Long volunteerId, Long eventId);
    List<Participation> findByStatus(ParticipationStatus status);
    List<Participation> findByEventIdAndStatus(Long eventId, ParticipationStatus status);
    List<Participation> findByVolunteerIdAndEventId(Long volunteerId, Long eventId);
}

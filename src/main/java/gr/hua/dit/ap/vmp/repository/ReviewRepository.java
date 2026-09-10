package gr.hua.dit.ap.vmp.repository;

import gr.hua.dit.ap.vmp.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByParticipationId(Long participationId);
    List<Review> findByParticipationEventOrganizationId(Long organizationId);
    List<Review> findByParticipationEventId(Long eventId);
    List<Review> findByRating(Integer rating);
    List<Review> findByParticipationEventIdAndRating(Long eventId, Integer rating);
    List<Review> findByParticipationEventOrganizationIdAndParticipationEventId(Long organizationId, Long eventId);
    List<Review> findByParticipationEventOrganizationIdAndRating(Long organizationId, Integer rating);
    List<Review> findByParticipationEventOrganizationIdAndParticipationEventIdAndRating(Long organizationId, Long eventId, Integer rating);
    List<Review> findByParticipationVolunteerId(Long volunteerId);
}

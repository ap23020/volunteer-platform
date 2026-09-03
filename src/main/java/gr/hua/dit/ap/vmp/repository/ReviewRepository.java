package gr.hua.dit.ap.vmp.repository;

import gr.hua.dit.ap.vmp.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByParticipationId(Long participationId);
}

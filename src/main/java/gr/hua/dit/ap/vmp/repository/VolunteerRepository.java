package gr.hua.dit.ap.vmp.repository;

import gr.hua.dit.ap.vmp.entities.UserStatus;
import gr.hua.dit.ap.vmp.entities.Volunteer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {
    List<Volunteer> findByStatus(UserStatus status);
    Page<Volunteer> findByStatus(UserStatus status, Pageable pageable);
    Optional<Volunteer> findByEmail(String email);
}

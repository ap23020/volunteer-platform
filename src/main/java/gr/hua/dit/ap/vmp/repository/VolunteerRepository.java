package gr.hua.dit.ap.vmp.repository;

import gr.hua.dit.ap.vmp.entities.UserStatus;
import gr.hua.dit.ap.vmp.entities.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {
    List<Volunteer> findByStatus(UserStatus status);
}

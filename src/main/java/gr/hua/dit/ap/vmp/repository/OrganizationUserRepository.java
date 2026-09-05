package gr.hua.dit.ap.vmp.repository;

import gr.hua.dit.ap.vmp.entities.OrganizationUser;
import gr.hua.dit.ap.vmp.entities.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationUserRepository extends JpaRepository<OrganizationUser, Long> {

    // Επιστρέφει όλους τους χρήστες ενός οργανισμού (ανεξαρτήτως κατάστασης)
    List<OrganizationUser> findByOrganizationId(Long organizationId);

    // Επιστρέφει χρήστες οργανισμού με συγκεκριμένη κατάσταση (π.χ. ACTIVE, PENDING, REJECTED)
    List<OrganizationUser> findByStatus(UserStatus status);

    // Επιστρέφει χρήστες ενός οργανισμού που έχουν συγκεκριμένη κατάσταση
    List<OrganizationUser> findByOrganizationIdAndStatus(Long organizationId, UserStatus status);
}
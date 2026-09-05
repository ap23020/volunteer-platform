package gr.hua.dit.ap.vmp.repository;

import gr.hua.dit.ap.vmp.entities.Organization;
import gr.hua.dit.ap.vmp.entities.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    // Επιστρέφει οργανισμό με συγκεκριμένο όνομα (για έλεγχο μοναδικότητας)
    Optional<Organization> findByName(String name);

    // Επιστρέφει οργανισμούς με συγκεκριμένη κατάσταση
    List<Organization> findByStatus(OrganizationStatus status);
}

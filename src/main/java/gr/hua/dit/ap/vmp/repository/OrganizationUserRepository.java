package gr.hua.dit.ap.vmp.repository;

import gr.hua.dit.ap.vmp.entities.OrganizationUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface OrganizationUserRepository extends JpaRepository<OrganizationUser, Long> {
    List<OrganizationUser> findByOrganizationId(Long organizationId);
}
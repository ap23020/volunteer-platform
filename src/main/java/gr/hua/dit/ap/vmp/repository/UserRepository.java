package gr.hua.dit.ap.vmp.repository;

import gr.hua.dit.ap.vmp.entities.Role;
import gr.hua.dit.ap.vmp.entities.User;
import gr.hua.dit.ap.vmp.entities.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByStatus(UserStatus status);

    List<User> findByRole(Role role);
}

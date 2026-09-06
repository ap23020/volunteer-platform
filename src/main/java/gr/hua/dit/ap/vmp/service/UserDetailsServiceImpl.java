package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.Role;
import gr.hua.dit.ap.vmp.entities.User;
import gr.hua.dit.ap.vmp.entities.UserStatus;
import gr.hua.dit.ap.vmp.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Βρίσκουμε τον χρήστη με βάση το email (το username για εμάς είναι το email)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Δημιουργούμε λίστα με authorities (δικαιώματα)
        List<GrantedAuthority> authorities = new ArrayList<>();
        Role role = user.getRole();
        if (role != null) {
            // Προσθέτουμε το ρόλο με το πρόθεμα ROLE_ που απαιτεί το Spring Security
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        }

        // Επιστρέφουμε ένα αντικείμενο UserDetails που περιέχει:
        // - username (email)
        // - password (κρυπτογραφημένο)
        // - enabled: true αν ο χρήστης είναι ACTIVE, αλλιώς false (δεν μπορεί να συνδεθεί)
        // - accountNonExpired, credentialsNonExpired, accountNonLocked: τα βάζουμε όλα true
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getStatus() == UserStatus.ACTIVE,   // enabled
                true,                                   // accountNonExpired
                true,                                   // credentialsNonExpired
                true,                                   // accountNonLocked
                authorities
        );
    }
}

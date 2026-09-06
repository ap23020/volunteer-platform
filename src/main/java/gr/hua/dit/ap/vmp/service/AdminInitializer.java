package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.Admin;
import gr.hua.dit.ap.vmp.entities.Role;
import gr.hua.dit.ap.vmp.entities.UserStatus;
import gr.hua.dit.ap.vmp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Δημιουργία ενός προεπιλεγμένου admin αν δεν υπάρχει ήδη κάποιος
        if (userRepository.findByRole(Role.ADMIN).isEmpty()) {
            Admin admin = new Admin();
            admin.setEmail("admin@volunteer.gr");
            admin.setPassword(passwordEncoder.encode("admin123"));   // κρυπτογράφηση
            admin.setPhone("2100000000");
            admin.setRole(Role.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setFirstName("System");
            admin.setLastName("Admin");
            userRepository.save(admin);
            System.out.println("Default admin created: admin@volunteer.gr / admin123");
        }
    }
}
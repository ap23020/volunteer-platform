package gr.hua.dit.ap.vmp.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    // Εμφανίζει τη σελίδα login
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
}
package gr.hua.dit.ap.vmp.controllers;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    @GetMapping
    public String home(Model model) {
        model.addAttribute("title", "Home");
        model.addAttribute("activePage", "home");
        return "index";
    }

    @GetMapping("/registration-pending")
    public String showPendingPage() {
        return "registration-pending";
    }
}

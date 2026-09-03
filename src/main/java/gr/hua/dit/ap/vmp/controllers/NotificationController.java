package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/list")
    public String listNotifications(Model model) {
        model.addAttribute("notifications", notificationService.getNotifications());
        model.addAttribute("activePage", "notifications");
        return "notification/notifications";
    }
}

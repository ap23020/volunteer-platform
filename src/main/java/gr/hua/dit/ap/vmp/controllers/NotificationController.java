package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.Notification;
import gr.hua.dit.ap.vmp.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/list")
    public String listNotifications(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    Model model) {
        Page<Notification> notificationPage = notificationService.getNotificationsPaginated(page, size);
        model.addAttribute("notificationPage", notificationPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notificationPage.getTotalPages());
        model.addAttribute("totalItems", notificationPage.getTotalElements());
        model.addAttribute("activePage", "notifications");
        return "notification/notifications";
    }
}

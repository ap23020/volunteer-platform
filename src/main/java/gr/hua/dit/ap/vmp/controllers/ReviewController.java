package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.*;
import gr.hua.dit.ap.vmp.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/review")
public class ReviewController {

    private final ReviewService reviewService;
    private final ParticipationService participationService;
    private final EventService eventService;
    private final UserService userService;
    private final VolunteerService volunteerService;

    public ReviewController(ReviewService reviewService,
                            ParticipationService participationService,
                            EventService eventService,
                            UserService userService,
                            VolunteerService volunteerService) {
        this.reviewService = reviewService;
        this.participationService = participationService;
        this.eventService = eventService;
        this.userService = userService;
        this.volunteerService = volunteerService;
    }

    // Λίστα όλων των αξιολογήσεων
    @GetMapping("/list")
    public String listReviews(@RequestParam(required = false) Long eventId,
                              @RequestParam(required = false) Integer rating,
                              Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        boolean isOrganization = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZATION"));
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Long organizationId = null;
        List<Event> events;

        if (isOrganization) {
            User user = userService.findByEmail(email);
            if (user instanceof OrganizationUser) {
                OrganizationUser orgUser = (OrganizationUser) user;
                organizationId = orgUser.getOrganization().getId();
                events = eventService.getEventsByOrganization(organizationId);
            } else {
                events = List.of();
            }
        } else {
            events = eventService.getEvents();
        }

        List<Review> reviews = reviewService.getFilteredReviews(organizationId, eventId, rating);

        model.addAttribute("reviews", reviews);
        model.addAttribute("events", events);
        model.addAttribute("ratings", List.of(1, 2, 3, 4, 5));
        model.addAttribute("selectedEventId", eventId);
        model.addAttribute("selectedRating", rating);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("activePage", "reviews");
        return "review/reviews";
    }

    // Φόρμα αξιολόγησης
    @GetMapping("/new")
    public String showReviewForm(@RequestParam Long participationId, Model model) {
        Participation participation = participationService.getParticipation(participationId);
        if (participation == null) {
            return "redirect:/participation/list";
        }
        model.addAttribute("participation", participation);
        model.addAttribute("activePage", "reviews");
        return "review/review-form";
    }

    // Υποβολή αξιολόγησης
    @PostMapping("/new")
    public String submitReview(@RequestParam Long participationId,
                               @RequestParam Integer rating,
                               @RequestParam(required = false) String comment,
                               RedirectAttributes redirectAttributes) {
        String error = reviewService.createReview(participationId, rating, comment);
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
            return "redirect:/review/new?participationId=" + participationId;
        }

        Participation participation = participationService.getParticipation(participationId);
        Long volunteerId = participation != null && participation.getVolunteer() != null
                ? participation.getVolunteer().getId() : null;

        redirectAttributes.addFlashAttribute("successMessage", "Review submitted successfully!");
        if (volunteerId != null) {
            return "redirect:/participation/volunteer/" + volunteerId;
        } else {
            return "redirect:/participation/list";
        }
    }
    @GetMapping("/my")
    public String myReviews(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Volunteer volunteer = volunteerService.getVolunteerByEmail(email);
        if (volunteer == null) {
            return "redirect:/";
        }

        List<Review> reviews = reviewService.getReviewsByVolunteer(volunteer.getId());
        model.addAttribute("reviews", reviews);
        model.addAttribute("activePage", "reviews");
        return "review/my-reviews";
    }
}
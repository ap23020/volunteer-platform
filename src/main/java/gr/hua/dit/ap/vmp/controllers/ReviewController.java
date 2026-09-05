package gr.hua.dit.ap.vmp.controllers;

import gr.hua.dit.ap.vmp.entities.Participation;
import gr.hua.dit.ap.vmp.entities.Review;
import gr.hua.dit.ap.vmp.service.ParticipationService;
import gr.hua.dit.ap.vmp.service.ReviewService;
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

    public ReviewController(ReviewService reviewService,
                            ParticipationService participationService) {
        this.reviewService = reviewService;
        this.participationService = participationService;
    }

    // Λίστα όλων των αξιολογήσεων
    @GetMapping("/list")
    public String listReviews(Model model) {
        model.addAttribute("reviews", reviewService.getReviews());
        model.addAttribute("activePage", "reviews");
        return "review/reviews";
    }

    // Εμφάνιση φόρμας αξιολόγησης για συγκεκριμένη συμμετοχή
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

        // Βρες τη συμμετοχή για να πάρεις το volunteerId
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
}
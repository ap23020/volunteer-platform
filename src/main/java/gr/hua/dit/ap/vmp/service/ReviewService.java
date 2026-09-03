package gr.hua.dit.ap.vmp.service;

import gr.hua.dit.ap.vmp.entities.*;
import gr.hua.dit.ap.vmp.repository.OrganizationUserRepository;
import gr.hua.dit.ap.vmp.repository.ParticipationRepository;
import gr.hua.dit.ap.vmp.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ParticipationRepository participationRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final NotificationService notificationService;

    public ReviewService(ReviewRepository reviewRepository,
                         ParticipationRepository participationRepository,
                         OrganizationUserRepository organizationUserRepository,
                         NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.participationRepository = participationRepository;
        this.organizationUserRepository = organizationUserRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public List<Review> getReviews() {
        return reviewRepository.findAll();
    }

    @Transactional
    public Review getReview(Long id) {
        return reviewRepository.findById(id).orElse(null);
    }

    @Transactional
    public void saveReview(Review review) {
        reviewRepository.save(review);
    }

    // Δημιουργία αξιολόγησης με ελέγχους
    @Transactional
    public String createReview(Long participationId, Integer rating, String comment) {
        Participation participation = participationRepository.findById(participationId).orElse(null);
        if (participation == null) {
            return "Participation not found.";
        }
        // Έλεγχος ότι η συμμετοχή είναι CHECKED_IN
        if (participation.getStatus() != ParticipationStatus.CHECKED_IN) {
            return "You can only review after check-in.";
        }
        // Έλεγχος ότι δεν υπάρχει ήδη αξιολόγηση
        if (reviewRepository.existsByParticipationId(participationId)) {
            return "This participation has already been reviewed.";
        }

        Review review = new Review(rating, comment, participation);
        reviewRepository.save(review);

        // Ειδοποίηση προς τον οργανισμό
        Event event = participation.getEvent();
        if (event != null) {
            Organization org = event.getOrganization();
            if (org != null) {
                List<OrganizationUser> orgUsers = organizationUserRepository.findByOrganizationId(org.getId());
                for (OrganizationUser orgUser : orgUsers) {
                    notificationService.createNotification(
                            NotificationType.NEW_REVIEW,
                            "New Review",
                            "A volunteer submitted a review for event \"" + event.getTitle() + "\".",
                            orgUser,
                            event
                    );
                }
            }
        }

        return null; // επιτυχία
    }
}


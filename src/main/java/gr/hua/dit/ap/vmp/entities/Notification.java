package gr.hua.dit.ap.vmp.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "email_sent")
    private boolean emailSent = false; // Προς το παρόν δεν στέλνουμε email

    @Column(name = "read")
    private boolean read = false;

    // Σχέση: Ο παραλήπτης (User)
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User recipient;

    // Σχέση: Σχετικό Event (προαιρετικό)
    @ManyToOne
    @JoinColumn(name = "event_id", referencedColumnName = "id")
    private Event relatedEvent;

    // Constructors
    public Notification() {}

    public Notification(NotificationType type, String title, String message, User recipient, Event relatedEvent) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.recipient = recipient;
        this.relatedEvent = relatedEvent;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }

    public Event getRelatedEvent() { return relatedEvent; }
    public void setRelatedEvent(Event relatedEvent) { this.relatedEvent = relatedEvent; }
}

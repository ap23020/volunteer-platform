package gr.hua.dit.ap.vmp.entities;

public enum NotificationType {
    // Ειδοποιήσεις σχετικά με προφίλ χρηστών
    PROFILE_APPROVED,
    PROFILE_REJECTED,

    // Ειδοποιήσεις σχετικά με εκδηλώσεις
    EVENT_APPROVED,
    EVENT_REJECTED,
    EVENT_CANCELLED,
    NEW_EVENT_REQUEST,          // <-- Νέο: όταν δημιουργείται event προς έγκριση

    // Ειδοποιήσεις σχετικά με συμμετοχές
    NEW_REGISTRATION,
    REGISTRATION_APPROVED,
    REGISTRATION_REJECTED,
    REGISTRATION_CANCELLED,

    // Ειδοποιήσεις σχετικά με αξιολογήσεις
    NEW_REVIEW,
    COMMENT_HIDDEN,

    // Ειδοποιήσεις σχετικά με οργανισμούς
    NEW_ORGANIZATION,
    ORGANIZATION_APPROVED,
    ORGANIZATION_REJECTED
}

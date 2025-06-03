package it.pagopa.pn.deliverypush.middleware.queue.consumer.router;

// Dovranno essere censiti qui tutti i tipi di eventi supportati dal router.
public enum SupportedEventType {
    NEW_NOTIFICATION,
    START_RECIPIENT_WORKFLOW,
    NOTIFICATION_VALIDATION,
    NOTIFICATION_REFUSED,
    SCHEDULE_RECEIVED_LEGALFACT_GENERATION
}

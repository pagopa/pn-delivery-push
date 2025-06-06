package it.pagopa.pn.deliverypush.middleware.queue.consumer.router;

// Dovranno essere censiti qui tutti i tipi di eventi supportati dal router.
public enum SupportedEventType {
    // Inizio eventi coda PnDeliveryPushInputs
    NOTIFICATION_PAID,
    NOTIFICATION_VIEWED,
    SEND_IO_MESSAGE_REQUEST,
    // Fine eventi coda PnDeliveryPushInputs
    // Inizio eventi coda ActionsValidation
    NOTIFICATION_VALIDATION,
    NOTIFICATION_REFUSED,
    SCHEDULE_RECEIVED_LEGALFACT_GENERATION,
    // Fine eventi coda ActionsValidation
    // Inizio eventi coda Actions
    NOTIFICATION_CANCELLATION,
    CHECK_ATTACHMENT_RETENTION,
    START_RECIPIENT_WORKFLOW,
    CHOOSE_DELIVERY_MODE,
    ANALOG_WORKFLOW,
    DIGITAL_WORKFLOW_NEXT_ACTION,
    DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION,
    DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION,
    DIGITAL_WORKFLOW_RETRY_ACTION,
    SEND_DIGITAL_FINAL_STATUS_RESPONSE,
    REFINEMENT_NOTIFICATION,
    DOCUMENT_CREATION_RESPONSE,
    POST_ACCEPTED_PROCESSING_COMPLETED,
    SEND_ANALOG_FINAL_STATUS_RESPONSE,
    // Fine eventi coda Actions
    // Inizio eventi coda ExternalChannels
    SEND_PEC_RESPONSE,
    SEND_ANALOG_RESPONSE,
    PREPARE_ANALOG_RESPONSE
    // Fine eventi coda ExternalChannels
}

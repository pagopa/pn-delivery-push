package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Getter
@Component
public class ActionHandlerRegistry {
    private final StartRecipientWorkflowHandler startRecipientWorkflowHandler;
    private final NotificationRefusedHandler notificationRefusedHandler;
    private final ChooseDeliveryModeEventHandler chooseDeliveryModeEventHandler;
    private final AnalogWorkflowEventHandler analogWorkflowEventHandler;
    private final RefinementNotificationHandler refinementNotificationHandler;
    private final DigitalWorkflowRetryActionHandler digitalWorkflowRetryActionHandler;
    private final DigitalWorkflowNoResponseTimeoutActionHandler digitalWorkflowNoResponseTimeoutActionHandler;
    private final NotificationValidationHandler notificationValidationHandler;
    private final ReceivedLegalFactGenerationHandler receivedLegalFactGenerationHandler;
    private final CheckAttachmentRetentionEventHandler checkAttachmentRetentionEventHandler;
    private final DigitalWorkflowNextActionHandler digitalWorkflowNextActionHandler;
    private final DigitalWorkflowNextExecuteActionHandler digitalWorkflowNextExecuteActionHandler;
    private final DocumentCreationResponseEventHandler documentCreationResponseEventHandler;
    private final SendDigitalFinalStatusResponseEventHandler sendDigitalFinalStatusResponseEventHandler;
    private final PostAcceptedProcessingCompletedHandler postAcceptedProcessingCompletedHandler;
    private final SendAnalogFinalStatusResponseHandler sendAnalogFinalStatusResponseHandler;
}

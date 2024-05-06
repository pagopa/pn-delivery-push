package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.checkattachmentretention.CheckAttachmentRetentionHandler;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action.details.*;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowRetryHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.SendDigitalFinalStatusResponseHandler;
import it.pagopa.pn.deliverypush.action.refinement.RefinementHandler;
import it.pagopa.pn.deliverypush.action.refused.NotificationRefusedActionHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationRequest;
import it.pagopa.pn.deliverypush.action.startworkflow.ScheduleRecipientWorkflow;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.StartWorkflowForRecipientHandler;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.responsehandler.DocumentCreationResponseHandler;
import it.pagopa.pn.deliverypush.utils.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;

@Slf4j
public class ActionHandlerMock {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final RefinementHandler refinementHandler;
    private final StartWorkflowForRecipientHandler startWorkflowForRecipientHandler;
    private final ChooseDeliveryModeHandler chooseDeliveryModeHandler;
    private final DocumentCreationResponseHandler documentCreationResponseHandler;
    private final NotificationValidationActionHandler notificationValidationActionHandler;
    private final ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest;
    private final NotificationRefusedActionHandler notificationRefusedActionHandler;
    private final CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;
    private final SendDigitalFinalStatusResponseHandler sendDigitalFinalStatusResponseHandler;
    private final ScheduleRecipientWorkflow scheduleRecipientWorkflow;
    
    public ActionHandlerMock(@Lazy DigitalWorkFlowHandler digitalWorkFlowHandler,
                                @Lazy DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler,
                                @Lazy AnalogWorkflowHandler analogWorkflowHandler,
                                @Lazy RefinementHandler refinementHandler,
                                @Lazy StartWorkflowForRecipientHandler startWorkflowForRecipientHandler,
                                @Lazy ChooseDeliveryModeHandler chooseDeliveryModeHandler,
                                @Lazy DocumentCreationResponseHandler documentCreationResponseHandler,
                                @Lazy NotificationValidationActionHandler notificationValidationActionHandler,
                                @Lazy ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest,
                                @Lazy NotificationRefusedActionHandler notificationRefusedActionHandler,
                                @Lazy CheckAttachmentRetentionHandler checkAttachmentRetentionHandler,
                                @Lazy SendDigitalFinalStatusResponseHandler sendDigitalFinalStatusResponseHandler,
                                @Lazy ScheduleRecipientWorkflow scheduleRecipientWorkflow) {
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.digitalWorkFlowRetryHandler = digitalWorkFlowRetryHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.refinementHandler = refinementHandler;
        this.startWorkflowForRecipientHandler = startWorkflowForRecipientHandler;
        this.chooseDeliveryModeHandler = chooseDeliveryModeHandler;
        this.documentCreationResponseHandler = documentCreationResponseHandler;
        this.notificationValidationActionHandler = notificationValidationActionHandler;
        this.receivedLegalFactCreationRequest = receivedLegalFactCreationRequest;
        this.notificationRefusedActionHandler = notificationRefusedActionHandler;
        this.checkAttachmentRetentionHandler = checkAttachmentRetentionHandler;
        this.sendDigitalFinalStatusResponseHandler = sendDigitalFinalStatusResponseHandler;
        this.scheduleRecipientWorkflow = scheduleRecipientWorkflow;
    }

    public void handleSchedulingAction(Action action) {
        ThreadPool.start(new Thread(() ->{
            switch (action.getType()) {
                case START_RECIPIENT_WORKFLOW ->
                        startWorkflowForRecipientHandler.startNotificationWorkflowForRecipient(action.getIun(), action.getRecipientIndex(),
                                (RecipientsWorkflowDetails) action.getDetails());
                case NOTIFICATION_REFUSED ->{
                    NotificationRefusedActionDetails notificationRefusedActionDetails = (NotificationRefusedActionDetails)  action.getDetails();
                    notificationRefusedActionHandler.notificationRefusedHandler(action.getIun(), notificationRefusedActionDetails.getErrors(), action.getNotBefore());
                }
                case CHOOSE_DELIVERY_MODE ->
                        chooseDeliveryModeHandler.chooseDeliveryTypeAndStartWorkflow(action.getIun(), action.getRecipientIndex());
                case ANALOG_WORKFLOW ->
                        analogWorkflowHandler.startAnalogWorkflow(action.getIun(), action.getRecipientIndex());
                case REFINEMENT_NOTIFICATION ->
                        refinementHandler.handleRefinement(action.getIun(), action.getRecipientIndex());
                case DIGITAL_WORKFLOW_RETRY_ACTION ->
                        digitalWorkFlowRetryHandler.startScheduledRetryWorkflow(action.getIun(), action.getRecipientIndex(),
                                action.getIun() + "_retry_action_" + action.getRecipientIndex());
                case DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION ->
                        digitalWorkFlowRetryHandler.elapsedExtChannelTimeout(action.getIun(), action.getRecipientIndex(),
                                action.getIun() + "_retry_action_" + action.getRecipientIndex());
                case NOTIFICATION_VALIDATION ->
                        notificationValidationActionHandler.validateNotification(action.getIun(), (NotificationValidationActionDetails)  action.getDetails());
                case SCHEDULE_RECEIVED_LEGALFACT_GENERATION ->
                        receivedLegalFactCreationRequest.saveNotificationReceivedLegalFacts(action.getIun());
                case CHECK_ATTACHMENT_RETENTION ->
                        checkAttachmentRetentionHandler.handleCheckAttachmentRetentionBeforeExpiration(action.getIun());
                case DIGITAL_WORKFLOW_NEXT_ACTION ->
                        digitalWorkFlowHandler.startScheduledNextWorkflow(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
                case DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION ->
                        digitalWorkFlowHandler.startNextWorkFlowActionExecute(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
                case DOCUMENT_CREATION_RESPONSE ->
                        documentCreationResponseHandler.handleResponseReceived(action.getIun(), action.getRecipientIndex(), (DocumentCreationResponseActionDetails)  action.getDetails());
                case SEND_DIGITAL_FINAL_STATUS_RESPONSE ->
                        sendDigitalFinalStatusResponseHandler.handleSendDigitalFinalStatusResponse(action.getIun(), (SendDigitalFinalStatusResponseDetails) action.getDetails());
                case POST_ACCEPTED_PROCESSING_COMPLETED ->
                        scheduleRecipientWorkflow.startScheduleRecipientWorkflow(action.getIun());
                default ->
                        log.error("[TEST] actionType not found {}", action.getType());
            }
        }));
    }

}

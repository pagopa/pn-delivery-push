package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;


import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogFinalStatusResponseHandler;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.cancellation.NotificationCancellationActionHandler;
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
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.EventRouter;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.responsehandler.DocumentCreationResponseHandler;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ACTIONTYPENOTSUPPORTED;


@Configuration
@AllArgsConstructor
@CustomLog
public class ActionHandler {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final RefinementHandler refinementHandler;
    private final StartWorkflowForRecipientHandler startWorkflowForRecipientHandler;
    private final ChooseDeliveryModeHandler chooseDeliveryModeHandler;
    private final DocumentCreationResponseHandler documentCreationResponseHandler;
    private final NotificationValidationActionHandler notificationValidationActionHandler;
    private final ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest;
    private final NotificationCancellationActionHandler notificationCancellationActionHandler;
    private final NotificationRefusedActionHandler notificationRefusedActionHandler;
    private final CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;
    private final TimelineUtils timelineUtils;
    private final SendDigitalFinalStatusResponseHandler sendDigitalFinalStatusResponseHandler;
    private final ScheduleRecipientWorkflow scheduleRecipientWorkflow;
    private final AnalogFinalStatusResponseHandler analogFinalResponseHandler;
    private final EventRouter eventRouter;
    
    @Bean
    public Consumer<Message<Action>> pnDeliveryPushStartRecipientWorkflow() {
        final String processName = ActionType.START_RECIPIENT_WORKFLOW.name();
        
        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushStartRecipientWorkflow, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
                log.logStartingProcess(processName);

                checkNotificationCancelledAndExecute(
                        action,
                        a -> startWorkflowForRecipientHandler.startNotificationWorkflowForRecipient(a.getIun(), a.getRecipientIndex(), (RecipientsWorkflowDetails) a.getDetails())
                );

                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushChooseDeliveryMode() {
        final String processName = ActionType.CHOOSE_DELIVERY_MODE.name();
        
        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushChooseDeliveryMode, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
                
                log.logStartingProcess(processName);
                checkNotificationCancelledAndExecute(
                        action,
                        a -> chooseDeliveryModeHandler.chooseDeliveryTypeAndStartWorkflow(a.getIun(), a.getRecipientIndex())
                );
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
    
    @Bean
    public Consumer<Message<Action>> pnDeliveryPushAnalogWorkflowConsumer() {
        final String processName = ActionType.ANALOG_WORKFLOW.name();
        
        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushAnalogWorkflowConsumer, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
                
                log.logStartingProcess(processName);
                checkNotificationCancelledAndExecute(
                        action,
                        a -> analogWorkflowHandler.startAnalogWorkflow(a.getIun(), a.getRecipientIndex())
                );
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushRefinementConsumer() {
        final String processName = ActionType.REFINEMENT_NOTIFICATION.name();
        
        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushRefinementConsumer, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

                log.logStartingProcess(processName);
                checkNotificationCancelledAndExecute(
                        action,
                        a -> refinementHandler.handleRefinement(a.getIun(), a.getRecipientIndex())
                );
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
    
    //From Scheduling
    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDigitalNextActionConsumer() {
        final String processName = ActionType.DIGITAL_WORKFLOW_NEXT_ACTION.name();
        
        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushDigitalNextActionConsumer, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

                log.logStartingProcess(processName);
                checkNotificationCancelledAndExecute(
                        action,
                        a ->digitalWorkFlowHandler.startScheduledNextWorkflow(a.getIun(), a.getRecipientIndex(), a.getTimelineId())
                );
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDigitalNextExecuteConsumer() {
        final String processName = ActionType.DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION.name();
        
        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushDigitalNextExecuteConsumer, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
                
                log.logStartingProcess(processName);
                checkNotificationCancelledAndExecute(
                        action,
                        a -> digitalWorkFlowHandler.startNextWorkFlowActionExecute(a.getIun(), a.getRecipientIndex(), a.getTimelineId())
                );
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }


    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDigitalRetryActionConsumer() {
        final String processName = ActionType.DIGITAL_WORKFLOW_RETRY_ACTION.name();
        
        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushDigitalRetryActionConsumer, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

                log.logStartingProcess(processName);
                checkNotificationCancelledAndExecute(
                        action,
                        a -> digitalWorkFlowRetryHandler.startScheduledRetryWorkflow(a.getIun(), a.getRecipientIndex(), a.getTimelineId())
                );
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushElapsedExternalChannelNoResponseTimeoutActionConsumer() {
        final String processName = ActionType.DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION.name();
        
        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushElapsedExternalChannelNoResponseTimeoutActionConsumer, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

                log.logStartingProcess(processName);
                checkNotificationCancelledAndExecute(
                        action,
                        a -> digitalWorkFlowRetryHandler.elapsedExtChannelTimeout(a.getIun(), a.getRecipientIndex(), a.getTimelineId())
                );
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDocumentCreationResponseConsumer() {
        final String processName = ActionType.DOCUMENT_CREATION_RESPONSE.name();
        
        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushDocumentCreationResponseConsumer, with content {}", message);
                Action action = message.getPayload();
                DocumentCreationResponseActionDetails details = (DocumentCreationResponseActionDetails) action.getDetails();
                MDC.put(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY, details.getKey());

                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
                log.logStartingProcess(processName);

                DocumentCreationResponseActionDetails documentCreationResponseActionDetails = (DocumentCreationResponseActionDetails) action.getDetails();
                if(DocumentCreationTypeInt.NOTIFICATION_CANCELLED.equals(documentCreationResponseActionDetails.getDocumentCreationType())){
                    //Solo se la si tratta della risposta alla generazione del documento di annullamento notifica viene bypassato il check di notifica annullata
                    documentCreationResponseHandler.handleResponseReceived(action.getIun(), action.getRecipientIndex(), documentCreationResponseActionDetails );
                }else {
                    checkNotificationCancelledAndExecute(
                            action,
                            a -> documentCreationResponseHandler.handleResponseReceived(a.getIun(), a.getRecipientIndex(), (DocumentCreationResponseActionDetails) a.getDetails() )
                    );
                }

                log.logEndingProcess(processName);

                MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushNotificationValidation() {
        final String processName = ActionType.NOTIFICATION_VALIDATION.name();
        
        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushNotificationValidation, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndCorrIdToMdc(action.getIun(), action.getActionId());
                log.logStartingProcess(processName);
                
                checkNotificationCancelledAndExecute(
                        action,
                        a -> notificationValidationActionHandler.validateNotification(a.getIun(), (NotificationValidationActionDetails) a.getDetails() )
                );
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushNotificationCancellation(){
        final String processName = ActionType.NOTIFICATION_CANCELLATION.name();

        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushNotificationCancellation, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndCorrIdToMdc(action.getIun(), action.getActionId());

                log.logStartingProcess(processName);
                notificationCancellationActionHandler.continueCancellationProcess(action.getIun());
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushReceivedLegalFactGeneration() {
        final String processName = ActionType.SCHEDULE_RECEIVED_LEGALFACT_GENERATION.name();
        
        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushReceivedLegalFactGeneration, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
                log.logStartingProcess(processName);
                checkNotificationCancelledAndExecute(
                        action,
                        a -> receivedLegalFactCreationRequest.saveNotificationReceivedLegalFacts(a.getIun())
                );
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushNotificationRefused(){
        final String processName = ActionType.NOTIFICATION_REFUSED.name();

        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushNotificationRefused, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndCorrIdToMdc(action.getIun(), action.getActionId());

                log.logStartingProcess(processName);
                NotificationRefusedActionDetails details = (NotificationRefusedActionDetails) action.getDetails();

                notificationRefusedActionHandler.notificationRefusedHandler(action.getIun(), details.getErrors(), action.getNotBefore());
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushCheckAttachmentRetention(){
        final String processName = ActionType.CHECK_ATTACHMENT_RETENTION.name();

        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushCheckAttachmentRetention, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndCorrIdToMdc(action.getIun(), action.getActionId());

                log.logStartingProcess(processName);

                checkAttachmentRetentionHandler.handleCheckAttachmentRetentionBeforeExpiration(action.getIun(), action.getNotBefore());
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushSendDigitalFinalStatusResponse(){
        final String processName = ActionType.SEND_DIGITAL_FINAL_STATUS_RESPONSE.name();

        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushSendDigitalFinalStatusResponse, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndCorrIdToMdc(action.getIun(), action.getActionId());

                log.logStartingProcess(processName);
                
                sendDigitalFinalStatusResponseHandler.handleSendDigitalFinalStatusResponse(action.getIun(), (SendDigitalFinalStatusResponseDetails) action.getDetails());
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushPostAcceptedProcessingCompleted(){
        final String processName = ActionType.POST_ACCEPTED_PROCESSING_COMPLETED.name();

        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushPostAcceptedProcessingCompleted, with content {}", message);
                log.logStartingProcess(processName);

                scheduleRecipientWorkflow.startScheduleRecipientWorkflow(message.getPayload().getIun());
                log.logEndingProcess(processName);
            }catch (Exception ex){
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushSendAnalogFinalStatusResponse(){
        final String processName = ActionType.SEND_ANALOG_FINAL_STATUS_RESPONSE.name();

        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushSendAnalogFinalStatusResponse, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndCorrIdToMdc(action.getIun(), action.getActionId());

                log.logStartingProcess(processName);
                analogFinalResponseHandler.handleFinalResponse(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushValidationActionsInboundConsumer() {
        final String processName = "VALIDATION_ACTIONS_INBOUND";

        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushValidationActionsInboundConsumer, with content {}", message);
                String actionType = extractActionType(message.getPayload());

                EventRouter.RoutingConfig routerConfig = EventRouter.RoutingConfig.builder()
                        .eventType(actionType)
                        .build();
                eventRouter.route(message, routerConfig);
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    private String extractActionType(Action action) {
        String actionType = action.getType() != null ? action.getType().name() : null;
        if (actionType == null) {
            log.error("actionType not present, cannot start scheduled action");
            throw new PnInternalException("actionType not present, cannot start scheduled action", ERROR_CODE_DELIVERYPUSH_ACTIONTYPENOTSUPPORTED);
        }

        return actionType;
    }


    private void checkNotificationCancelledAndExecute(Action action, Consumer<Action> functionToCall) {
        if (! timelineUtils.checkIsNotificationCancellationRequested(action.getIun())) {
            functionToCall.accept(action);
        } else {
            log.info("Notification is cancelled, the action will not be executed - iun={}", action.getIun());
        }
    }

}

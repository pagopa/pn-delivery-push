package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.cancellation.NotificationCancellationActionHandler;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.details.NotificationRefusedActionDetails;
import it.pagopa.pn.deliverypush.action.details.NotificationValidationActionDetails;
import it.pagopa.pn.deliverypush.action.details.RecipientsWorkflowDetails;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowRetryHandler;
import it.pagopa.pn.deliverypush.action.refinement.RefinementHandler;
import it.pagopa.pn.deliverypush.action.refused.NotificationRefusedActionHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationRequest;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.StartWorkflowForRecipientHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.impl.WebhookActionsEventHandler;
import it.pagopa.pn.deliverypush.middleware.responsehandler.DocumentCreationResponseHandler;
import it.pagopa.pn.deliverypush.utils.MdcKey;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@AllArgsConstructor
@CustomLog
public class ActionHandler {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final RefinementHandler refinementHandler;
    private final WebhookActionsEventHandler webhookActionsEventHandler;
    private final StartWorkflowForRecipientHandler startWorkflowForRecipientHandler;
    private final ChooseDeliveryModeHandler chooseDeliveryModeHandler;
    private final DocumentCreationResponseHandler documentCreationResponseHandler;
    private final NotificationValidationActionHandler notificationValidationActionHandler;
    private final ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest;
    private final NotificationCancellationActionHandler notificationCancellationActionHandler;
    private final NotificationRefusedActionHandler notificationRefusedActionHandler;
    private final TimelineUtils timelineUtils;

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushStartRecipientWorkflow() {
        final String processName = "START RECIPIENT WORKFLOW";
        
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
        final String processName = "CHOOSE DELIVERY MODE";
        
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
        final String processName = "START ANALOG WORKFLOW";
        
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
        final String processName = "HANDLE REFINEMENT";
        
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

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDigitalNextActionConsumer() {
        final String processName = "SCHEDULED NEXT WORKFLOW ACTION";
        
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
        final String processName = "START NEXT WORKFLOW ACTION";
        
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
        final String processName = "SCHEDULED RETRY WORKFLOW";
        
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
        final String processName = "EXTERNAL CHANNEL NO RESPONSE TIMEOUT ACTION";
        
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
    public Consumer<Message<WebhookAction>> pnDeliveryPushWebhookActionConsumer() {
        final String processName = "WEBHOOK ACTION";
        
        return message -> {
            try {
                MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);

                log.debug("Handle action pnDeliveryPushWebhookActionConsumer, with content {}", message);log.debug("pnDeliveryPushWebhookActionConsumer, message={}", message);
                WebhookAction action = message.getPayload();
                HandleEventUtils.addIunToMdc(action.getIun());

                log.logStartingProcess(processName);
                webhookActionsEventHandler.handleEvent(action);
                log.logEndingProcess(processName);

                MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDocumentCreationResponseConsumer() {
        final String processName = "DOCUMENT CREATION RESPONSE";
        
        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushDocumentCreationResponseConsumer, with content {}", message);
                Action action = message.getPayload();
                DocumentCreationResponseActionDetails details = (DocumentCreationResponseActionDetails) action.getDetails();
                MDC.put(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY, details.getKey());

                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
                
                log.logStartingProcess(processName);
                checkNotificationCancelledAndExecute(
                        action,
                        a -> documentCreationResponseHandler.handleResponseReceived(a.getIun(), a.getRecipientIndex(), (DocumentCreationResponseActionDetails) a.getDetails() )
                );
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
        final String processName = "NOTIFICATION VALIDATION";
        
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
        final String processName = "NOTIFICATION CANCELLATION";

        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushNotificationCancellation, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndCorrIdToMdc(action.getIun(), action.getActionId());

                log.logStartingProcess(processName);
                notificationCancellationActionHandler.cancelNotification(action.getIun());
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
        final String processName = "SAVE NOTIFICATION RECEIVED LEGALFACT";
        
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
        final String processName = "NOTIFICATION REFUSED";

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
    
    private void checkNotificationCancelledAndExecute(Action action, Consumer<Action> functionToCall) {
        if (! timelineUtils.checkIsNotificationCancellationRequested(action.getIun())) {
            functionToCall.accept(action);
        } else {
            log.info("Notification is cancelled, the action will not be executed - iun={}", action.getIun());
        }
    }

}

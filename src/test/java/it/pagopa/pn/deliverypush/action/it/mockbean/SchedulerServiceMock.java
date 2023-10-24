package it.pagopa.pn.deliverypush.action.it.mockbean;


import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
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
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.middleware.responsehandler.DocumentCreationResponseHandler;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.utils.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@Slf4j
public class SchedulerServiceMock implements SchedulerService {
  private final DigitalWorkFlowHandler digitalWorkFlowHandler;
  private final DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler;
  private final AnalogWorkflowHandler analogWorkflowHandler;
  private final RefinementHandler refinementHandler;
  private final InstantNowSupplier instantNowSupplier;
  private final StartWorkflowForRecipientHandler startWorkflowForRecipientHandler;
  private final ChooseDeliveryModeHandler chooseDeliveryModeHandler;
  private final DocumentCreationResponseHandler documentCreationResponseHandler;
  private final NotificationValidationActionHandler notificationValidationActionHandler;
  private final ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest;
  private final NotificationRefusedActionHandler notificationRefusedActionHandler;

  public SchedulerServiceMock(@Lazy DigitalWorkFlowHandler digitalWorkFlowHandler,
                              @Lazy DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler,
                              @Lazy AnalogWorkflowHandler analogWorkflowHandler,
                              @Lazy RefinementHandler refinementHandler,
                              @Lazy InstantNowSupplier instantNowSupplier,
                              @Lazy StartWorkflowForRecipientHandler startWorkflowForRecipientHandler,
                              @Lazy ChooseDeliveryModeHandler chooseDeliveryModeHandler,
                              @Lazy DocumentCreationResponseHandler documentCreationResponseHandler,
                              @Lazy NotificationValidationActionHandler notificationValidationActionHandler,
                              @Lazy ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest,
                              @Lazy NotificationRefusedActionHandler notificationRefusedActionHandler) {
    this.digitalWorkFlowHandler = digitalWorkFlowHandler;
    this.digitalWorkFlowRetryHandler = digitalWorkFlowRetryHandler;
    this.analogWorkflowHandler = analogWorkflowHandler;
    this.refinementHandler = refinementHandler;
    this.instantNowSupplier = instantNowSupplier;
    this.startWorkflowForRecipientHandler = startWorkflowForRecipientHandler;
    this.chooseDeliveryModeHandler = chooseDeliveryModeHandler;
    this.documentCreationResponseHandler = documentCreationResponseHandler;
    this.notificationValidationActionHandler = notificationValidationActionHandler;
    this.receivedLegalFactCreationRequest = receivedLegalFactCreationRequest;
    this.notificationRefusedActionHandler = notificationRefusedActionHandler;
  }

  @Override
  public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule,
      ActionType actionType, ActionDetails actionDetails) {
    log.info("[TEST] Start scheduling - iun={} id={} actionType={} ", iun, recIndex, actionType);

    ThreadPool.start(new Thread(() -> {
      
      Assertions.assertDoesNotThrow(() -> {
        waitSchedulingTime(dateToSchedule);

        switch (actionType) {
          case START_RECIPIENT_WORKFLOW -> 
                  startWorkflowForRecipientHandler.startNotificationWorkflowForRecipient(iun, recIndex,
                  (RecipientsWorkflowDetails) actionDetails);
          case NOTIFICATION_REFUSED ->{
            NotificationRefusedActionDetails notificationRefusedActionDetails = (NotificationRefusedActionDetails) actionDetails;
            notificationRefusedActionHandler.notificationRefusedHandler(iun, notificationRefusedActionDetails.getErrors(),dateToSchedule);
          }
          case CHOOSE_DELIVERY_MODE ->
                  chooseDeliveryModeHandler.chooseDeliveryTypeAndStartWorkflow(iun, recIndex);
          case ANALOG_WORKFLOW ->
                  analogWorkflowHandler.startAnalogWorkflow(iun, recIndex);
          case REFINEMENT_NOTIFICATION ->
                  refinementHandler.handleRefinement(iun, recIndex);
          case DIGITAL_WORKFLOW_NEXT_ACTION -> 
                  digitalWorkFlowHandler.startScheduledNextWorkflow(iun, recIndex, null);
          case DIGITAL_WORKFLOW_RETRY_ACTION ->
                  digitalWorkFlowRetryHandler.startScheduledRetryWorkflow(iun, recIndex,
                  iun + "_retry_action_" + recIndex);
          case DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION ->
                  digitalWorkFlowRetryHandler.elapsedExtChannelTimeout(iun, recIndex,
                  iun + "_retry_action_" + recIndex);
          case NOTIFICATION_VALIDATION ->
                  notificationValidationActionHandler.validateNotification(iun, (NotificationValidationActionDetails) actionDetails);
          case SCHEDULE_RECEIVED_LEGALFACT_GENERATION ->
                  receivedLegalFactCreationRequest.saveNotificationReceivedLegalFacts(iun);
          default ->
                  log.error("[TEST] actionType not found {}", actionType);
        }
      });
    }));

  }

  private void waitSchedulingTime(Instant dateToSchedule) throws InterruptedException {
    log.info("[TEST] DateToSchedule {} instantNow = {}", dateToSchedule, Instant.now());

    await()
            .atMost(100, TimeUnit.SECONDS)
            .untilAsserted(() -> Assertions.assertTrue(Instant.now().isAfter(dateToSchedule)));
  }

  @Override
  public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule,
      ActionType actionType, String timelineId) {
    log.info("[TEST] Start scheduling with timelineid - iun={} id={} actionType={} timelineid={} datetoschedule={}", iun, recIndex, actionType, timelineId, dateToSchedule);

    ThreadPool.start(new Thread(() -> {
      Assertions.assertDoesNotThrow(() -> {
        mockSchedulingDate(dateToSchedule);

        switch (actionType) {

          case DIGITAL_WORKFLOW_NEXT_ACTION ->
            digitalWorkFlowHandler.startScheduledNextWorkflow(iun, recIndex, timelineId);
          case DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION ->
            digitalWorkFlowHandler.startNextWorkFlowActionExecute(iun, recIndex, timelineId);
        /*case DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION:
          digitalWorkFlowRetryHandler.elapsedExtChannelTimeout(iun, recIndex,
                  timelineId);
          break;*/
        }
      });
    }));
  }

  @Override
  public void unscheduleEvent(String iun, Integer recIndex, ActionType actionType,
      String timelineId) {
    // non usato come mock
  }

  @Override
  public void scheduleWebhookEvent(String paId, String iun, String timelineId) {
    // non usato come mock
  }

  @Override
  public void scheduleWebhookEvent(String streamId, String eventId, Integer delay,
      WebhookEventType actionType) {
    // non usato come mock
  }

  private void mockSchedulingDate(Instant dateToSchedule) {
    Instant previousCurrentTime = instantNowSupplier.get();
    Instant schedulingDate = dateToSchedule.plus(1, ChronoUnit.HOURS);
    if (previousCurrentTime.isAfter(schedulingDate))
      schedulingDate = previousCurrentTime.plus(1, ChronoUnit.HOURS);

    Mockito.when(instantNowSupplier.get()).thenReturn(schedulingDate);
    log.info("[TEST] mockSchedulingDate instantNow is {}" , schedulingDate);
  }

  @Override
  public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule,
      ActionType actionType, String timelineId, ActionDetails actionDetails) {
    if(timelineId != null && actionDetails != null){

      ThreadPool.start( new Thread(() -> {

        Assertions.assertDoesNotThrow(() -> {
          waitSchedulingTime(dateToSchedule);

          switch (actionType) {
            case DOCUMENT_CREATION_RESPONSE ->
                    documentCreationResponseHandler.handleResponseReceived(iun, recIndex, (DocumentCreationResponseActionDetails) actionDetails);
            default -> 
                    log.error("[TEST] actionType not found {}", actionType);
          }
        });
      }));

      
    }else {
      
    }
    if (timelineId == null)
      this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, actionDetails);
    else
      this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, timelineId);

  }

  @Override
  public void scheduleEvent(String iun, Instant dateToSchedule, ActionType actionType, ActionDetails actionDetails) {
    this.scheduleEvent(iun, null, dateToSchedule, actionType, actionDetails);
  }

  @Override
  public void scheduleEvent(String iun, Instant dateToSchedule, ActionType actionType){
    this.scheduleEvent(iun, null, dateToSchedule, actionType);
  }

  @Override
  public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule,
      ActionType actionType) {
    this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, (ActionDetails) null);
  }

}

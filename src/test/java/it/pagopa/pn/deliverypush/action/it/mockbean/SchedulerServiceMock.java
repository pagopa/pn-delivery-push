package it.pagopa.pn.deliverypush.action.it.mockbean;


import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action.details.RecipientsWorkflowDetails;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowRetryHandler;
import it.pagopa.pn.deliverypush.action.refinement.RefinementHandler;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.StartWorkflowForRecipientHandler;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
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

  public SchedulerServiceMock(@Lazy DigitalWorkFlowHandler digitalWorkFlowHandler,
      @Lazy DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler,
      @Lazy AnalogWorkflowHandler analogWorkflowHandler, @Lazy RefinementHandler refinementHandler,
      @Lazy InstantNowSupplier instantNowSupplier,
      @Lazy StartWorkflowForRecipientHandler startWorkflowForRecipientHandler,
      @Lazy ChooseDeliveryModeHandler chooseDeliveryModeHandler) {
    this.digitalWorkFlowHandler = digitalWorkFlowHandler;
    this.digitalWorkFlowRetryHandler = digitalWorkFlowRetryHandler;
    this.analogWorkflowHandler = analogWorkflowHandler;
    this.refinementHandler = refinementHandler;
    this.instantNowSupplier = instantNowSupplier;
    this.startWorkflowForRecipientHandler = startWorkflowForRecipientHandler;
    this.chooseDeliveryModeHandler = chooseDeliveryModeHandler;
  }

  @Override
  public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule,
      ActionType actionType, ActionDetails actionDetails) {
    log.info("Start scheduling - iun={} id={} actionType={} ", iun, recIndex, actionType);

    new Thread(() -> {
      
      Assertions.assertDoesNotThrow(() -> {
        waitSchedulingTime(dateToSchedule);

        switch (actionType) {
          case START_RECIPIENT_WORKFLOW -> 
                  startWorkflowForRecipientHandler.startNotificationWorkflowForRecipient(iun, recIndex,
                  (RecipientsWorkflowDetails) actionDetails);
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
        }
      });
    }).start();
  }

  private void waitSchedulingTime(Instant dateToSchedule) throws InterruptedException {
    log.info("DateToSchedule {} instantNow = {}", dateToSchedule, Instant.now());

    await()
            .atMost(100, TimeUnit.SECONDS)
            .untilAsserted(() -> Assertions.assertTrue(Instant.now().isAfter(dateToSchedule)));
  }

  @Override
  public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule,
      ActionType actionType, String timelineId) {
    log.info("Start scheduling with timelineid - iun={} id={} actionType={} timelineid={} ", iun, recIndex, actionType, timelineId);

    new Thread(() -> {
      Assertions.assertDoesNotThrow(() -> {
        mockSchedulingDate(dateToSchedule);

        switch (actionType) {

          case DIGITAL_WORKFLOW_NEXT_ACTION:
            digitalWorkFlowHandler.startScheduledNextWorkflow(iun, recIndex, timelineId);
            break;
        /*case DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION:
          digitalWorkFlowRetryHandler.elapsedExtChannelTimeout(iun, recIndex,
                  timelineId);
          break;*/
        }
      });
    }).start();
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
    Instant schedulingDate = dateToSchedule.plus(1, ChronoUnit.HOURS);
    Mockito.when(instantNowSupplier.get()).thenReturn(schedulingDate);
  }

  @Override
  public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule,
      ActionType actionType, String timelineId, ActionDetails actionDetails) {
    if (timelineId == null)
      this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, actionDetails);
    else
      this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, timelineId);

  }

  @Override
  public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule,
      ActionType actionType) {
    this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, (ActionDetails) null);

  }

}

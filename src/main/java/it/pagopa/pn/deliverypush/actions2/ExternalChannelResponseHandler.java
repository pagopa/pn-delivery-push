package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.extchannel.ExtChannelDigitalResponse;
import it.pagopa.pn.api.dto.extchannel.ExternalChannelAnalogResponse;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.deliverypush.actions.PecFailSendPaperActionHandler;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.StreamListener;

@Slf4j
public class ExternalChannelResponseHandler {
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    private AnalogWorkflowHandler analogWorkflowHandler;
    private TimelineService timelineService;
    private CompletionWorkFlowHandler completionWorkflow;


    /**
     * Handle Digital notification response from external channel. Positive response means notification is delivered correctly, so the workflow can be completed successfully.
     * Negative response means notification could not be delivered to the indicated address so need to start next workflow action.
     *
     * @param response Notification response
     */
    @StreamListener(condition = "EXTERNAL_CHANNEL_RESPONSE_DIGITAL")
    public void extChannelResponseReceiverForDigital(ExtChannelDigitalResponse response) {
        log.info("Get response from externachannel for iun {} id {} with status {}", response.getIun(), response.getTaxId(), response.getResponseStatus());

        //Conservare ricevuta PEC //TODO capire cosa si intende

        switch (response.getResponseStatus()) {
            case OK:
                //Se viene la notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
                completionWorkflow.completionDigitalWorkflow(response.getTaxId(), response.getIun(), response.getNotificationDate(), EndWorkflowStatus.SUCCESS);
                break;
            case KO:
                //Se external channel non è riuscito ad effettuare la notificazione, si passa alla prossima action del worklfow
                addDigitalFailureAttemptToTimeline(response);
                digitalWorkFlowHandler.nextWorkFlowAction(response.getIun(), response.getTaxId());
                break;
        }
    }

    private void addDigitalFailureAttemptToTimeline(ExtChannelDigitalResponse response) {
        timelineService.addTimelineElement(TimelineElement.builder()
                .iun(response.getIun())
                .category(TimelineElementCategory.SEND_DIGITAL_FEEDBACK_FAILURE)
                .details(SendDigitalFeedbackFailure.builder()
                        .downstreamId(response.getDownstreamId())
                        .errors(response.getErrorList())
                        .address(response.getAddress())
                        .taxId(response.getTaxId())
                        .build())
                .build());
    }

    /**
     * Handle Analog notification response from external channel. Positive response means notification is delivered correctly, so the workflow can be completed successfully,
     * negative response means notification could not be delivered to the indicated address so need to start next workflow action.
     *
     * @param response Notification response
     */
    @StreamListener(condition = "EXTERNAL_CHANNEL_RESPONSE_ANALOG")
    public void extChannelResponseReceiverForAnalog(ExternalChannelAnalogResponse response) {

        switch (response.getResponseStatus()) {
            case OK:
                //Se viene la notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
                completionWorkflow.completionAnalogWorkflow(response.getTaxId(), response.getIun(), response.getNotificationDate(), EndWorkflowStatus.SUCCESS);
                break;
            case KO:
                //Se external channel non è riuscito ad effettuare la notificazione, si passa alla prossima action del worklfow
                addAnalogFailureAttemptToTimeline(response);
                analogWorkflowHandler.nextWorkflowAction(response.getIun(), response.getTaxId());
                break;
        }
    }

    private void addAnalogFailureAttemptToTimeline(ExternalChannelAnalogResponse response) {
        timelineService.addTimelineElement(TimelineElement.builder()
                .category(TimelineElementCategory.SEND_PAPER_FEEDBACK)
                .details(new SendPaperFeedbackDetails(
                        SendPaperDetails.builder()
                                .taxId(response.getTaxId())
                                .address(response.getUsedAddress())
                                .serviceLevel(PecFailSendPaperActionHandler.DIGITAL_FAILURE_PAPER_FALLBACK_SERVICE_LEVEL) //TODO Capirne il senso
                                .build(),
                        response.getNewAddressFromInvestigation(),
                        response.getAttachmentKeys(),
                        response.getErrorList()

                ))
                .build());
    }

}

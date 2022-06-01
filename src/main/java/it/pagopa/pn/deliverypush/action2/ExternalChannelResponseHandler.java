package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.CourtesyMessageProgressEvent;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.LegalMessageSentDetails;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExternalChannelResponseHandler {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final ExternalChannelUtils externalChannelUtils;
    private final TimelineUtils timelineUtils;

    public ExternalChannelResponseHandler(DigitalWorkFlowHandler digitalWorkFlowHandler, AnalogWorkflowHandler analogWorkflowHandler,
                                          ExternalChannelUtils externalChannelUtils, TimelineUtils timelineUtils) {
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.externalChannelUtils = externalChannelUtils;
        this.timelineUtils = timelineUtils;
    }

    /**
     * Handle notification response from external channel. Positive response means notification is delivered correctly, so the workflow can be completed successfully.
     * Negative response means notification could not be delivered to the indicated address.
     *
     * @param response Notification response
     */
    public void extChannelResponseReceiver(SingleStatusUpdate response) {
        if (response.getDigitalCourtesy() != null)
            courtesyUpdate(response.getDigitalCourtesy());
        else if (response.getDigitalLegal() != null)
            legalUpdate(response.getDigitalLegal());
        else if (response.getAnalogMail() != null)
            paperUpdate(response.getAnalogMail());
        else
            handleError(response);
    }


    private void paperUpdate(PaperProgressStatusEvent event)
    {
        log.info("Received PaperProgressStatusEvent event for requestId={} - status={} details={} deliveryfailcause={}", event.getRequestId(), event.getStatusCode(), event.getStatusDescription(), event.getDeliveryFailureCause());
        String iun = timelineUtils.getIunFromTimelineId(event.getRequestId());
        TimelineElementInternal notificationTimelineElement = externalChannelUtils.getExternalChannelNotificationTimelineElement(iun, event.getRequestId());

        analogWorkflowHandler.extChannelResponseHandler(event, notificationTimelineElement);

    }

    private void legalUpdate(LegalMessageSentDetails event)
    {
        log.info("Received LegalMessageSentDetails event for requestId={} - status={} details={} eventcode={}", event.getRequestId(), event.getStatus(), event.getEventDetails(), event.getEventCode());
        String iun = timelineUtils.getIunFromTimelineId(event.getRequestId());
        TimelineElementInternal notificationTimelineElement = externalChannelUtils.getExternalChannelNotificationTimelineElement(iun, event.getRequestId());

        digitalWorkFlowHandler.handleExternalChannelResponse(event, notificationTimelineElement);
    }


    private void courtesyUpdate(CourtesyMessageProgressEvent event)
    {
        // per ora non è previsto nulla
        log.info("Received CourtesyMessageProgressEvent event for requestId={} - status={} details={} eventcode={}", event.getRequestId(), event.getStatus(), event.getEventDetails(), event.getEventCode());
    }

    private void handleError(SingleStatusUpdate response) {
        log.error("None event specified in extchannelevent event={}", response);
        throw new PnInternalException("None event specified, invalid event update received from external-channel");
    }

}


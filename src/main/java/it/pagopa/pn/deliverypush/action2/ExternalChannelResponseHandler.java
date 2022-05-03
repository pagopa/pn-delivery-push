package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExternalChannelResponseHandler {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final ExternalChannelUtils externalChannelUtils;

    public ExternalChannelResponseHandler(DigitalWorkFlowHandler digitalWorkFlowHandler, AnalogWorkflowHandler analogWorkflowHandler,
                                          ExternalChannelUtils externalChannelUtils) {
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.externalChannelUtils = externalChannelUtils;
    }

    /**
     * Handle notification response from external channel. Positive response means notification is delivered correctly, so the workflow can be completed successfully.
     * Negative response means notification could not be delivered to the indicated address.
     *
     * @param response Notification response
     */
    public void extChannelResponseReceiver(ExtChannelResponse response) {
        log.info("Get response from external channel with status {} - iun {} eventId {} ", response.getResponseStatus(), response.getIun(), response.getEventId());
        TimelineElement notificationTimelineElement = externalChannelUtils.getExternalChannelNotificationTimelineElement(response.getIun(), response.getEventId());

        log.debug("Get notification element ok, category {} - iun {} eventId {} ", notificationTimelineElement.getCategory(), response.getIun(), response.getEventId());

        if (notificationTimelineElement.getCategory() != null) {
            switch (notificationTimelineElement.getCategory()) {
                case SEND_DIGITAL_DOMICILE:
                    digitalWorkFlowHandler.handleExternalChannelResponse(response, notificationTimelineElement);
                    break;
                case SEND_ANALOG_DOMICILE:
                    analogWorkflowHandler.extChannelResponseHandler(response, notificationTimelineElement);
                    break;
                case SEND_SIMPLE_REGISTERED_LETTER:
                    //Non richiede azioni specifiche
                    log.info("Received SEND_SIMPLE_REGISTERED_LETTER response for response status {} - iun {} eventId {} ", response.getResponseStatus(), response.getIun(), response.getEventId());
                    break;
                default:
                    handleError(response, notificationTimelineElement);
                    break;
            }
        } else {
            handleError(response, notificationTimelineElement);
        }
    }

    private void handleError(ExtChannelResponse response, TimelineElement notificationTimelineElement) {
        log.error("Specified category {} is not possibile - iun {} eventId {}", notificationTimelineElement.getCategory(), response.getIun(), response.getEventId());
        throw new PnInternalException("Specified category " + notificationTimelineElement.getCategory() + " is not possibile");
    }

}


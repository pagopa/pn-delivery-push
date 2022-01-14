package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.StreamListener;
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
    @StreamListener(condition = "EXTERNAL_CHANNEL_RESPONSE")
    public void extChannelResponseReceiver(ExtChannelResponse response) {
        log.info("Get response from external channel for iun {} eventId {} with status {}", response.getIun(), response.getEventId(), response.getResponseStatus());
        TimelineElement notificationTimelineElement = externalChannelUtils.getExternalChannelNotificationTimelineElement(response.getIun(), response.getEventId());

        if (response.getDigitalUsedAddress() != null && response.getDigitalUsedAddress().getAddress() != null) {
            digitalWorkFlowHandler.handleExternalChannelResponse(response, notificationTimelineElement);
        } else {
            analogWorkflowHandler.extChannelResponseHandler(response, notificationTimelineElement);
        }
    }

}


package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExternalChannelResponseHandler {
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    private AnalogWorkflowHandler analogWorkflowHandler;

    public ExternalChannelResponseHandler(DigitalWorkFlowHandler digitalWorkFlowHandler, AnalogWorkflowHandler analogWorkflowHandler) {
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
    }

    /**
     * Handle notification response from external channel. Positive response means notification is delivered correctly, so the workflow can be completed successfully.
     * Negative response means notification could not be delivered to the indicated address.
     *
     * @param response Notification response
     */
    @StreamListener(condition = "EXTERNAL_CHANNEL_RESPONSE")
    public void extChannelResponseReceiverForDigital(ExtChannelResponse response) {
        log.info("Get response from external channel for iun {} id {} with status {}", response.getIun(), response.getTaxId(), response.getResponseStatus());
        if (response.getDigitalUsedAddress() != null)
            digitalWorkFlowHandler.handleExternalChannelResponse(response);
        else
            analogWorkflowHandler.extChannelResponseHandler(response);
    }

}

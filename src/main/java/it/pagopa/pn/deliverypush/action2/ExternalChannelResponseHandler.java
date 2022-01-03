package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExternalChannelResponseHandler {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;

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
    public void extChannelResponseReceiver(ExtChannelResponse response) {
        log.info("Get response from external channel for iun {} id {} with status {}", response.getIun(), response.getTaxId(), response.getResponseStatus());
        //TODO Verificare se è possibile verificare in un altro modo il digitale e l'analogico
        
        if (response.getDigitalUsedAddress() != null && response.getDigitalUsedAddress().getAddress() != null)
            digitalWorkFlowHandler.handleExternalChannelResponse(response);
        else
            analogWorkflowHandler.extChannelResponseHandler(response);
    }

}

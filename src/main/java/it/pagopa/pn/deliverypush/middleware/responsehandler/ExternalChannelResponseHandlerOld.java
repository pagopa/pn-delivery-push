package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatusEvent;
import it.pagopa.pn.api.dto.events.PnExtChnProgressStatusEventPayload;
import it.pagopa.pn.deliverypush.action.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.AttachmentDetailsInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelAnalogSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelProgressEventCat;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * @deprecated
 * Deprecata in attesa di un mock di externalChannel con le nuove api
 */
@Deprecated(since = "PN-612", forRemoval = true)
@Slf4j
@Component
public class ExternalChannelResponseHandlerOld {

    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final ExternalChannelUtils externalChannelUtils;

    public ExternalChannelResponseHandlerOld(DigitalWorkFlowHandler digitalWorkFlowHandler, AnalogWorkflowHandler analogWorkflowHandler,
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
    public void extChannelResponseReceiver(PnExtChnProgressStatusEvent response) {
        log.info("Get response from external channel with status {} - iun {} eventId {} ", response.getPayload().getStatusCode(), response.getPayload().getIun(), response.getPayload().getRequestCorrelationId());
        TimelineElementInternal notificationTimelineElement = externalChannelUtils.getExternalChannelNotificationTimelineElement(response.getPayload().getIun(), response.getPayload().getRequestCorrelationId());

        log.debug("Get notification element ok, category {} - iun {} eventId {} ", notificationTimelineElement.getCategory(), response.getPayload().getIun(), response.getPayload().getRequestCorrelationId());

        
        if (notificationTimelineElement.getCategory() != null) {
            switch (notificationTimelineElement.getCategory()) {
                case SEND_DIGITAL_DOMICILE:
                    ExtChannelDigitalSentResponseInt responseDigital = mapToDigital(response.getPayload());
                    digitalWorkFlowHandler.handleExternalChannelResponse(responseDigital);
                    break;
                case SEND_ANALOG_DOMICILE:
                    ExtChannelAnalogSentResponseInt responseAnalog = mapToAnalog(response.getPayload());
                    analogWorkflowHandler.extChannelResponseHandler(responseAnalog);
                    break;
                case SEND_SIMPLE_REGISTERED_LETTER:
                    //Non richiede azioni specifiche
                    break;
                default:
                    log.error("ERROR");
                    break;
            }
        } else {
            log.error("ERROR");
        }
    }

    private ExtChannelAnalogSentResponseInt mapToAnalog(PnExtChnProgressStatusEventPayload response) {

        ExtChannelProgressEventCat status = getStatusCode(response);

        PhysicalAddressInt newPhysicalAddress = null;
        it.pagopa.pn.api.dto.notification.address.PhysicalAddress newPhysicalAddressExt = response.getNewPhysicalAddress();
        if(newPhysicalAddressExt != null){
            newPhysicalAddress = PhysicalAddressInt.builder()
                    .address(newPhysicalAddressExt.getAddress())
                    .province(newPhysicalAddressExt.getProvince())
                    .addressDetails(newPhysicalAddressExt.getAddressDetails())
                    .municipality(newPhysicalAddressExt.getMunicipality())
                    .at(newPhysicalAddressExt.getAt())
                    .zip(newPhysicalAddressExt.getZip())
                    .foreignState(newPhysicalAddressExt.getForeignState())
                    .build();
        }
        
        return ExtChannelAnalogSentResponseInt.builder()
                .iun(response.getIun())
                .requestId(response.getRequestCorrelationId())
                .statusCode(status.getValue())
                .discoveredAddress(newPhysicalAddress)
                .attachments(
                        response.getAttachmentKeys().stream().map( elem -> AttachmentDetailsInt.builder()
                            .url(elem)
                            .build()
                        ).collect(Collectors.toList())
                )
                .statusDateTime(response.getStatusDate())
                .build();
    }

    private ExtChannelDigitalSentResponseInt mapToDigital(PnExtChnProgressStatusEventPayload response) {
        ExtChannelProgressEventCat status = getStatusCode(response);

        return ExtChannelDigitalSentResponseInt.builder()
                .iun(response.getIun())
                .requestId(response.getRequestCorrelationId())
                .eventTimestamp(response.getStatusDate())
                .status(status)
                .eventCode("DEFAULT")
                .eventDetails("DEFAULT")
                .build();
    }

    @NotNull
    private ExtChannelProgressEventCat getStatusCode(PnExtChnProgressStatusEventPayload response) {
        ExtChannelProgressEventCat status;
        switch(response.getStatusCode()){
            case OK:
                status = ExtChannelProgressEventCat.OK;
                break;
            case PERMANENT_FAIL:
                status =  ExtChannelProgressEventCat.ERROR;
                break;
            case RETRYABLE_FAIL:
                status =  ExtChannelProgressEventCat.RETRIABLE_ERROR;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + response.getStatusCode());
        }
        return status;
    }


}

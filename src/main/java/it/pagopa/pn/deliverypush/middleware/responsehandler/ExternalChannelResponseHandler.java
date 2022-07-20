package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.*;
import it.pagopa.pn.deliverypush.action.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@Slf4j
public class ExternalChannelResponseHandler {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final TimelineUtils timelineUtils;

    public ExternalChannelResponseHandler(DigitalWorkFlowHandler digitalWorkFlowHandler,
                                          AnalogWorkflowHandler analogWorkflowHandler,
                                          TimelineUtils timelineUtils) {
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
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
        try {
            ExtChannelAnalogSentResponseInt analogSentResponseInt = mapExternalToInternal(event);
            
            log.info("Received PaperProgressStatusEvent event for requestId={} - status={} details={} deliveryfailcause={}",
                    analogSentResponseInt.getRequestId(), analogSentResponseInt.getStatusCode(), analogSentResponseInt.getStatusDescription(), analogSentResponseInt.getDeliveryFailureCause());
            
            analogWorkflowHandler.extChannelResponseHandler(analogSentResponseInt);
        } catch (PnInternalException e) {
            log.error("PnException legalUpdate", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception legalUpdate", e);
            throw new PnInternalException("Exception on legalUpdate", e);
        }

    }

    private ExtChannelAnalogSentResponseInt mapExternalToInternal(PaperProgressStatusEvent event) {
        ExtChannelAnalogSentResponseInt.ExtChannelAnalogSentResponseIntBuilder builder = ExtChannelAnalogSentResponseInt.builder()
                .statusCode(event.getStatusCode())
                .deliveryFailureCause(event.getDeliveryFailureCause())
                .iun(event.getIun())
                .requestId(event.getRequestId())
                .statusDateTime(event.getStatusDateTime().toInstant())
                .statusDescription(event.getStatusDescription())
                ;
        
        if ( event.getDiscoveredAddress() != null){
            DiscoveredAddress rawAddress = event.getDiscoveredAddress();

            builder.discoveredAddress(
                    PhysicalAddressInt.builder()
                            .address(rawAddress.getAddress())
                            .addressDetails(rawAddress.getAddressRow2())
                            .municipality(rawAddress.getCity())
                            .municipalityDetails(rawAddress.getCity2())
                            .province(rawAddress.getPr())
                            .zip(rawAddress.getCap())
                            .foreignState(rawAddress.getCountry())
                            .at(rawAddress.getNameRow2())
                    .build()
            );
        }
        
        if ( event.getAttachments() != null){
            builder.attachments(
                    event.getAttachments().stream().map(
                            att -> AttachmentDetailsInt.builder()
                                        .date(att.getDate().toInstant())
                                        .id(att.getId())
                                        .documentType(att.getDocumentType())
                                        .url(att.getUrl())
                                        .build()
                    ).collect(Collectors.toList())
            );
        }
        
        return builder.build();
    }

    private void legalUpdate(LegalMessageSentDetails event)
    {
        try {
            String iun = timelineUtils.getIunFromTimelineId(event.getRequestId());

            ExtChannelDigitalSentResponseInt digitalSentResponseInt = mapExternalToInternal(event, iun);
            log.info("Received LegalMessageSentDetails event for requestId={} - status={} details={} eventCode={}", 
                    digitalSentResponseInt.getRequestId(), digitalSentResponseInt.getStatus(), digitalSentResponseInt.getEventDetails(), digitalSentResponseInt.getEventCode());
            
            digitalWorkFlowHandler.handleExternalChannelResponse(digitalSentResponseInt);
        } catch (PnInternalException e) {
            log.error("Exception legalUpdate", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception legalUpdate", e);
            throw new PnInternalException("Exception on legalUpdate", e);
        }
    }

    private ExtChannelDigitalSentResponseInt mapExternalToInternal(LegalMessageSentDetails event, String iun) {
        ExtChannelDigitalSentResponseInt.ExtChannelDigitalSentResponseIntBuilder builder = ExtChannelDigitalSentResponseInt.builder()
                .iun(iun)
                .eventDetails(event.getEventDetails())
                .eventTimestamp(event.getEventTimestamp().toInstant())
                .status( ExtChannelProgressEventCat.valueOf(event.getStatus().getValue()))
                .eventCode(event.getEventCode())
                .requestId(event.getRequestId());
        
        if(event.getGeneratedMessage() != null){
            builder.digitalMessageReferenceInt( DigitalMessageReferenceInt.builder()
                    .location(event.getGeneratedMessage().getLocation())
                    .system(event.getGeneratedMessage().getSystem())
                    .id(event.getGeneratedMessage().getId())
                    .build()
            );
        }
        
        return builder.build();
    }

    private void courtesyUpdate(CourtesyMessageProgressEvent event)
    {
        try {
            // per ora non è previsto nulla
            log.info("Received CourtesyMessageProgressEvent event for requestId={} - status={} details={} eventcode={}", event.getRequestId(), event.getStatus(), event.getEventDetails(), event.getEventCode());
        } catch (PnInternalException e) {
            log.error("Exception legalUpdate", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception legalUpdate", e);
            throw new PnInternalException("Exception on legalUpdate", e);
        }
    }

    private void handleError(SingleStatusUpdate response) {
        log.error("None event specified in extchannelevent event={}", response);
        throw new PnInternalException("None event specified, invalid event update received from external-channel");
    }

}


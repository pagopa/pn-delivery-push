package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.*;
import it.pagopa.pn.deliverypush.action.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.DigitalWorkFlowExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_UPDATEFAILED;

@Component
@Slf4j
public class ExternalChannelResponseHandler {
    private final DigitalWorkFlowExternalChannelResponseHandler digitalWorkFlowExternalChannelResponseHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final TimelineUtils timelineUtils;

    public ExternalChannelResponseHandler(DigitalWorkFlowExternalChannelResponseHandler digitalWorkFlowExternalChannelResponseHandler,
                                          AnalogWorkflowHandler analogWorkflowHandler,
                                          TimelineUtils timelineUtils) {
        this.digitalWorkFlowExternalChannelResponseHandler = digitalWorkFlowExternalChannelResponseHandler;
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

    private void paperUpdate(PaperProgressStatusEvent event) {
        try {
            ExtChannelAnalogSentResponseInt analogSentResponseInt = mapExternalToInternal(event);

            log.info("Received ExternalChannel paper message event for requestId={} - status={} details={} deliveryfailcause={}",
                    analogSentResponseInt.getRequestId(), analogSentResponseInt.getStatusCode(), analogSentResponseInt.getStatusDescription(), analogSentResponseInt.getDeliveryFailureCause());

            analogWorkflowHandler.extChannelResponseHandler(analogSentResponseInt);
        } catch (PnRuntimeException e) {
            log.error("PnException legalUpdate", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception legalUpdate", e);
            throw new PnInternalException("Paper update failed", ERROR_CODE_DELIVERYPUSH_UPDATEFAILED, e);
        }

    }

    private ExtChannelAnalogSentResponseInt mapExternalToInternal(PaperProgressStatusEvent event) {
        ExtChannelAnalogSentResponseInt.ExtChannelAnalogSentResponseIntBuilder builder = ExtChannelAnalogSentResponseInt.builder()
                .statusCode(event.getStatusCode())
                .deliveryFailureCause(event.getDeliveryFailureCause())
                .iun(event.getIun())
                .requestId(event.getRequestId())
                .statusDateTime(event.getStatusDateTime().toInstant())
                .statusDescription(event.getStatusDescription());

        if (event.getDiscoveredAddress() != null) {
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

        if (event.getAttachments() != null) {
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

    private void legalUpdate(LegalMessageSentDetails event) {
        try {
            String iun = timelineUtils.getIunFromTimelineId(event.getRequestId());

            ExtChannelDigitalSentResponseInt digitalSentResponseInt = mapExternalToInternal(event, iun);
            log.info("Received ExternalChannel legal message event for requestId={} - status={} details={} eventCode={} generatedMessage={} eventTimestamp={}",
                    digitalSentResponseInt.getRequestId(), digitalSentResponseInt.getStatus(), digitalSentResponseInt.getEventDetails(), digitalSentResponseInt.getEventCode(),
                    digitalSentResponseInt.getGeneratedMessage(), digitalSentResponseInt.getEventTimestamp());
            
            digitalWorkFlowExternalChannelResponseHandler.handleExternalChannelResponse(digitalSentResponseInt);
        } catch (PnInternalException e) {
            log.error("Exception legalUpdate", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception legalUpdate", e);
            throw new PnInternalException("Legal update failed", ERROR_CODE_DELIVERYPUSH_UPDATEFAILED, e);
        }
    }

    private ExtChannelDigitalSentResponseInt mapExternalToInternal(LegalMessageSentDetails event, String iun) {
        ExtChannelDigitalSentResponseInt.ExtChannelDigitalSentResponseIntBuilder builder = ExtChannelDigitalSentResponseInt.builder()
                .iun(iun)
                .eventDetails(event.getEventDetails())
                .eventTimestamp(event.getEventTimestamp().toInstant())
                .status(ExtChannelProgressEventCat.valueOf(event.getStatus().getValue()))
                .eventCode(EventCodeInt.valueOf(event.getEventCode().getValue()))
                .requestId(event.getRequestId());

        if (event.getGeneratedMessage() != null) {
            builder.generatedMessage(DigitalMessageReferenceInt.builder()
                    .location(event.getGeneratedMessage().getLocation())
                    .system(event.getGeneratedMessage().getSystem())
                    .id(event.getGeneratedMessage().getId())
                    .build()
            );
        }

        return builder.build();
    }

    private void courtesyUpdate(CourtesyMessageProgressEvent event) {
        try {
            // per ora non è previsto nulla
            log.info("Received ExternalChannel courtesy message event for requestId={} - status={} details={} eventcode={}", event.getRequestId(), event.getStatus(), event.getEventDetails(), event.getEventCode());
        } catch (PnInternalException e) {
            log.error("Courtesy update failed", e);
            throw e;
        } catch (Exception e) {
            log.error("Courtesy update failed", e);
            throw new PnInternalException("Courtesy update failed", ERROR_CODE_DELIVERYPUSH_UPDATEFAILED, e);
        }
    }

    private void handleError(SingleStatusUpdate response) {
        log.error("None event specified in extchannelevent event={}", response);
        throw new PnInternalException("None event specified, invalid event update received from external-channel", ERROR_CODE_DELIVERYPUSH_UPDATEFAILED);
    }

}


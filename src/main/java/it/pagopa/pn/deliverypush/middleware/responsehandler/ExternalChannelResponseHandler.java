package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.CourtesyMessageProgressEvent;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.LegalMessageSentDetails;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.DigitalMessageReferenceInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelProgressEventCat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_UPDATEFAILED;

@Component
@Slf4j
public class ExternalChannelResponseHandler {
    public static final String EXCEPTION_LEGAL_UPDATE = "Exception legalUpdate";
    public static final String COURTESY_UPDATE_FAILED = "Courtesy update failed";
    private final DigitalWorkFlowExternalChannelResponseHandler digitalWorkFlowExternalChannelResponseHandler;
    private final TimelineUtils timelineUtils;

    public ExternalChannelResponseHandler(DigitalWorkFlowExternalChannelResponseHandler digitalWorkFlowExternalChannelResponseHandler,
                                          TimelineUtils timelineUtils) {
        this.digitalWorkFlowExternalChannelResponseHandler = digitalWorkFlowExternalChannelResponseHandler;
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
        else
            handleError(response);
    }


    private void legalUpdate(LegalMessageSentDetails event) {
        try {
            String iun = timelineUtils.getIunFromTimelineId(event.getRequestId());

            ExtChannelDigitalSentResponseInt digitalSentResponseInt = mapExternalToInternal(event, iun);
            log.info("Received ExternalChannel legal message event status={} and eventCode={} - iun={} requestId={} details={} generatedMessage={} eventTimestamp={}",
                    digitalSentResponseInt.getStatus(), digitalSentResponseInt.getEventCode(), iun, digitalSentResponseInt.getRequestId(), digitalSentResponseInt.getEventDetails(),
                    digitalSentResponseInt.getGeneratedMessage(), digitalSentResponseInt.getEventTimestamp());
            
            digitalWorkFlowExternalChannelResponseHandler.handleExternalChannelResponse(digitalSentResponseInt);
        } catch (PnInternalException e) {
            log.error(EXCEPTION_LEGAL_UPDATE, e);
            throw e;
        } catch (Exception e) {
            log.error(EXCEPTION_LEGAL_UPDATE, e);
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
            log.error(COURTESY_UPDATE_FAILED, e);
            throw e;
        } catch (Exception e) {
            log.error(COURTESY_UPDATE_FAILED, e);
            throw new PnInternalException(COURTESY_UPDATE_FAILED, ERROR_CODE_DELIVERYPUSH_UPDATEFAILED, e);
        }
    }

    private void handleError(SingleStatusUpdate response) {
        log.error("None event specified in extchannelevent event={}", response);
        throw new PnInternalException("None event specified, invalid event update received from external-channel", ERROR_CODE_DELIVERYPUSH_UPDATEFAILED);
    }

}


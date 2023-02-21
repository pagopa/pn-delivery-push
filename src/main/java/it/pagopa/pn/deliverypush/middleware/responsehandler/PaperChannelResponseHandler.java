package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.model.AnalogAddress;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.model.PaperChannelUpdate;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.model.PrepareEvent;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.model.SendEvent;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowPaperChannelResponseHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.AttachmentDetailsInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.PrepareEventInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendEventInt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED;


@Component
@Slf4j
public class PaperChannelResponseHandler {

    public static final String EXCEPTION_PREPARE_UPDATE = "Exception PrepareUpdate";
    public static final String EXCEPTION_SEND_UPDATE = "Exception SendUpdate";
    private final AnalogWorkflowPaperChannelResponseHandler analogWorkflowPaperChannelResponseHandler;
    private final TimelineUtils timelineUtils;

    public PaperChannelResponseHandler(AnalogWorkflowPaperChannelResponseHandler analogWorkflowPaperChannelResponseHandler,
                                       TimelineUtils timelineUtils) {
        this.analogWorkflowPaperChannelResponseHandler = analogWorkflowPaperChannelResponseHandler;
        this.timelineUtils = timelineUtils;
    }

    /**
     * Handle notification response from external channel. Positive response means notification is delivered correctly, so the workflow can be completed successfully.
     * Negative response means notification could not be delivered to the indicated address.
     *
     * @param response Notification response
     */
    public void paperChannelResponseReceiver(PaperChannelUpdate response) {
        if (response.getPrepareEvent() != null)
            prepareUpdate(response.getPrepareEvent());
        else if (response.getSendEvent() != null)
            sendUpdate(response.getSendEvent());
        else
            handleError(response);
    }

    private void prepareUpdate(PrepareEvent event) {
        try {
            String iun = timelineUtils.getIunFromTimelineId(event.getRequestId());

            PrepareEventInt analogSentResponseInt = mapExternalToInternal(iun, event);

            log.info("Received PaperChannel prepare paper message event for requestId={} - status={} details={} receiverAddress={}",
                    analogSentResponseInt.getRequestId(), analogSentResponseInt.getStatusCode(), analogSentResponseInt.getStatusDetail(), (analogSentResponseInt.getReceiverAddress()==null?"":LogUtils.maskGeneric(analogSentResponseInt.getReceiverAddress().getAddress())));

            analogWorkflowPaperChannelResponseHandler.paperChannelPrepareResponseHandler(analogSentResponseInt);
        } catch (PnRuntimeException e) {
            log.error(EXCEPTION_PREPARE_UPDATE, e);
            throw e;
        } catch (Exception e) {
            log.error(EXCEPTION_PREPARE_UPDATE, e);
            throw new PnInternalException("Paper update failed", ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED, e);
        }

    }


    private void sendUpdate(SendEvent event) {
        try {
            String iun = timelineUtils.getIunFromTimelineId(event.getRequestId());

            SendEventInt analogSentResponseInt = mapExternalToInternal(iun, event);

            log.info("Received PaperChannel send paper message event for requestId={} - status={} details={} discovAddress={}",
                    analogSentResponseInt.getRequestId(), analogSentResponseInt.getStatusCode(), analogSentResponseInt.getStatusDetail(), (analogSentResponseInt.getDiscoveredAddress()==null?"":LogUtils.maskGeneric(analogSentResponseInt.getDiscoveredAddress().getAddress())));

            analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(analogSentResponseInt);
        } catch (PnRuntimeException e) {
            log.error(EXCEPTION_SEND_UPDATE, e);
            throw e;
        } catch (Exception e) {
            log.error(EXCEPTION_SEND_UPDATE, e);
            throw new PnInternalException("Paper update failed", ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED, e);
        }

    }

    private SendEventInt mapExternalToInternal(String iun, SendEvent event) {
        var builder = SendEventInt.builder()
                .iun(iun)
                .statusCode(event.getStatusCode())
                .statusDetail(event.getStatusDetail())
                .deliveryFailureCause(event.getDeliveryFailureCause())
                .requestId(event.getRequestId())
                .statusDateTime(event.getStatusDateTime().toInstant())
                .statusDescription(event.getStatusDescription());

        if (event.getDiscoveredAddress() != null) {
            AnalogAddress rawAddress = event.getDiscoveredAddress();

            builder.discoveredAddress(
                    PhysicalAddressInt.builder()
                            .fullname(rawAddress.getFullname())
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
                    ).toList()
            );
        }

        return builder.build();
    }

    private PrepareEventInt mapExternalToInternal(String iun, PrepareEvent event) {
        var builder = PrepareEventInt.builder()
                .iun(iun)
                .statusCode(event.getStatusCode()==null?null:event.getStatusCode().getValue())
                .statusDetail(event.getStatusDetail())
                .requestId(event.getRequestId())
                .statusDateTime(event.getStatusDateTime().toInstant())
                .productType(event.getProductType());

        if (event.getReceiverAddress() != null) {
            AnalogAddress rawAddress = event.getReceiverAddress();

            builder.receiverAddress(
                    PhysicalAddressInt.builder()
                            .fullname(rawAddress.getFullname())
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


        return builder.build();
    }


    private void handleError(PaperChannelUpdate response) {
        log.error("None event specified in paperchannelevent event={}", response);
        throw new PnInternalException("None event specified, invalid event update received from paper-channel", ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
    }

}


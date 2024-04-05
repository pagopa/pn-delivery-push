package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.CategorizedAttachmentsResultInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.ResultFilterInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.*;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowPaperChannelResponseHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.AttachmentDetailsInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.PrepareEventInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendEventInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED;


@Component
@CustomLog
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
        String iun = timelineUtils.getIunFromTimelineId(event.getRequestId());
        addMdcFilter(iun, event.getRequestId());
        log.info("Async response received from service {} for {} with correlationId={}",
                PaperChannelSendClient.CLIENT_NAME, PaperChannelSendClient.PREPARE_ANALOG_NOTIFICATION, event.getRequestId());

        final String processName = PaperChannelSendClient.PREPARE_ANALOG_NOTIFICATION + " response handler";
        
        try {
            log.logStartingProcess(processName);

            PrepareEventInt analogSentResponseInt = mapExternalToInternal(iun, event);

            log.debug("Received PaperChannel prepare paper message event for requestId={} - status={} details={} receiverAddress={}",
                    analogSentResponseInt.getRequestId(), analogSentResponseInt.getStatusCode(), analogSentResponseInt.getStatusDetail(), (analogSentResponseInt.getReceiverAddress()==null?"":LogUtils.maskGeneric(analogSentResponseInt.getReceiverAddress().getAddress())));

            analogWorkflowPaperChannelResponseHandler.paperChannelPrepareResponseHandler(analogSentResponseInt);
            
            log.logEndingProcess(processName);

        } catch (PnRuntimeException e) {
            log.logEndingProcess(processName, false, e.getMessage());
            log.error(EXCEPTION_PREPARE_UPDATE, e);
            throw e;
        } catch (Exception e) {
            log.logEndingProcess(processName, false, e.getMessage());
            log.error(EXCEPTION_PREPARE_UPDATE, e);
            throw new PnInternalException("Paper update failed", ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED, e);
        }
    }

    private void sendUpdate(SendEvent event) {
        
        String iun = timelineUtils.getIunFromTimelineId(event.getRequestId());
        addMdcFilter(iun, event.getRequestId());
        log.info("Async response received from service {} for {} with correlationId={}",
                PaperChannelSendClient.CLIENT_NAME, PaperChannelSendClient.SEND_ANALOG_NOTIFICATION, event.getRequestId());

        final String processName = PaperChannelSendClient.SEND_ANALOG_NOTIFICATION + " response handler";
        
        try {
            log.logStartingProcess(processName);

            SendEventInt analogSentResponseInt = mapExternalToInternal(iun, event);
            log.info("Received PaperChannel send paper message event for requestId={} - status={} details={} discovAddress={}",
                    analogSentResponseInt.getRequestId(), analogSentResponseInt.getStatusCode(), analogSentResponseInt.getStatusDetail(), (analogSentResponseInt.getDiscoveredAddress()==null?"":LogUtils.maskGeneric(analogSentResponseInt.getDiscoveredAddress().getAddress())));

            analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(analogSentResponseInt);
            log.logEndingProcess(processName);
        } catch (PnRuntimeException e) {
            log.logEndingProcess(processName, false, e.getMessage());
            log.error(EXCEPTION_SEND_UPDATE, e);
            throw e;
        } catch (Exception e) {
            log.logEndingProcess(processName, false, e.getMessage());
            log.error(EXCEPTION_SEND_UPDATE, e);
            throw new PnInternalException("Paper update failed", ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED, e);
        }

    }

    private SendEventInt mapExternalToInternal(String iun, SendEvent event) {
        var builder = SendEventInt.builder()
                .iun(iun)
                .statusCode(event.getStatusCode()==null?null:event.getStatusCode().getValue())
                .statusDetail(event.getStatusDetail())
                .deliveryFailureCause(event.getDeliveryFailureCause())
                .requestId(event.getRequestId())
                .registeredLetterCode(event.getRegisteredLetterCode())
                .statusDateTime(event.getStatusDateTime())
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
                                    .date(att.getDate())
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

        // valido l'evento
        validateEvent(event);

        var builder = PrepareEventInt.builder()
                .iun(iun)
                .statusCode(Optional.ofNullable(event.getStatusCode()).map(StatusCodeEnum::getValue).orElse(null))
                .statusDetail(event.getStatusDetail())
                .replacedF24AttachmentUrls(event.getReplacedF24AttachmentUrls())
                .requestId(event.getRequestId())
                .statusDateTime(event.getStatusDateTime())
                .failureDetailCode(Optional.ofNullable(event.getFailureDetailCode()).map(FailureDetailCodeEnum::getValue).orElse(null))
                .productType(event.getProductType());

        if (event.getCategorizedAttachments() != null) {
            CategorizedAttachmentsResult rawCategorizedAttachments = event.getCategorizedAttachments();

            List<ResultFilterInt> acceptedAttachments = rawCategorizedAttachments.getAcceptedAttachments() == null ? null :
                    rawCategorizedAttachments.getAcceptedAttachments().stream()
                        .map(this::mapResultFilterToInternal)
                        .collect(Collectors.toList());

            List<ResultFilterInt> discardedAttachments = rawCategorizedAttachments.getDiscardedAttachments() == null ? null :
                    rawCategorizedAttachments.getDiscardedAttachments().stream()
                            .map(this::mapResultFilterToInternal)
                            .collect(Collectors.toList());

            builder.categorizedAttachmentsResult(
                    CategorizedAttachmentsResultInt.builder()
                            .acceptedAttachments(acceptedAttachments)
                            .discardedAttachments(discardedAttachments)
                            .build()
            );
        }

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

    private ResultFilterInt mapResultFilterToInternal(ResultFilter resultFilter){
        return ResultFilterInt.builder()
                .fileKey(resultFilter.getFileKey())
                .result(resultFilter.getResult())
                .reasonCode(resultFilter.getReasonCode())
                .reasonDescription(resultFilter.getReasonDescription())
                .build();
    }

    private void validateEvent(PrepareEvent event){
        // mi aspetto ci sia lo statusCode
        if (event.getStatusCode() == null)
        {
            log.error("No statusCode specified in paperchannelevent event={}", event);
            throw new PnInternalException("No statusCode specified, invalid event update received from paper-channel", ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
        }
        if (event.getStatusCode() == StatusCodeEnum.KO)
        {
            // mi aspetto ci sia il failureDetailCode
            if (event.getFailureDetailCode() == null) {
                log.error("No failureDetailCode specified in paperchannelevent event={}", event);
                throw new PnInternalException("No failureDetailCode specified, invalid event update received from paper-channel", ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
            }
            // nel caso di D01, D02, mi aspetto ci sia anche l'indirizzo
            if ((event.getFailureDetailCode() == FailureDetailCodeEnum.D01 || event.getFailureDetailCode() == FailureDetailCodeEnum.D02)
                && event.getReceiverAddress() == null)
            {
                log.error("No address specified in paperchannelevent event={}", event);
                throw new PnInternalException("No address specified, invalid event update received from paper-channel", ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
            }
        }
    }

    private void handleError(PaperChannelUpdate response) {
        log.error("None event specified in paperchannelevent event={}", response);
        throw new PnInternalException("None event specified, invalid event update received from paper-channel", ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
    }

    private static void addMdcFilter(String iun, String correlationId) {
        HandleEventUtils.addIunToMdc(iun);
        HandleEventUtils.addCorrelationIdToMdc(correlationId);
    }
}


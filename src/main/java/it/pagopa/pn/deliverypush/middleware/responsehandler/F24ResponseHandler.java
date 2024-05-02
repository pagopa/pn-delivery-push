package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.exceptions.PnValidationNotValidF24Exception;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24.PnF24Client;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.F24Service;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@CustomLog
@AllArgsConstructor
public class F24ResponseHandler {
    private TimelineUtils timelineUtils;
    private NotificationValidationActionHandler validationActionHandler;
    private final F24Service f24Service;
    private final SchedulerService schedulerService;
    private static final String PATH_TOKEN_SEPARATOR = "_";


    public void handleEventF24(DetailedTypePayload event){
        if(event instanceof PnF24MetadataValidationEndEvent.Detail metadataValidationEndEvent) {
            log.info("Handle event PnF24MetadataValidationEndEvent to handleResponseReceived");
            handleResponseReceived(metadataValidationEndEvent);
        }else if(event instanceof PnF24PdfSetReadyEvent.Detail pdfSetReadyEvent){
            log.info("Handle event PnF24PdfSetReadyEvent to handlePrepareResponseReceived");
            handlePrepareResponseReceived(pdfSetReadyEvent);
        }else{
            throw new PnInternalException("Invalid type for handleMessageF24", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE);
        }
    }

    private void handleResponseReceived(PnF24MetadataValidationEndEvent.Detail event) {
        if (event.getMetadataValidationEnd() != null) {
            PnF24MetadataValidationEndEventPayload metadataValidationEndEvent = event.getMetadataValidationEnd();
            String iun = metadataValidationEndEvent.getSetId();
            addMdcFilter(iun);
            log.info("Async response received from service {} for {} with iun={}",
                    PnF24Client.CLIENT_NAME, PnF24Client.VALIDATE_F24_PROCESS_NAME, event.getMetadataValidationEnd().getSetId());

            final String processName = PnF24Client.VALIDATE_F24_PROCESS_NAME + " response handler";

            if (timelineUtils.checkIsNotificationCancellationRequested(iun)) {
                log.warn("Process {} blocked: cancellation requested for iun {}", processName, iun);
                return;
            }

            try {
                log.logStartingProcess(processName);
                validationActionHandler.handleValidateF24Response(metadataValidationEndEvent);
                log.logEndingProcess(processName);
            } catch (Exception ex){
                log.logEndingProcess(processName, false, ex.getMessage());
                throw ex;
            }
        }else{
            throw new PnValidationNotValidF24Exception("invalid event payload");
        }
    }

    private void handlePrepareResponseReceived(PnF24PdfSetReadyEvent.Detail event){
        PnF24PdfSetReadyEventPayload pdfSetReady = event.getPdfSetReady();

        List<PnF24PdfSetReadyEventItem> generatedPdfsUrls = pdfSetReady.getGeneratedPdfsUrls();
        String timelineId = pdfSetReady.getRequestId();
        String iunFromTimelineId = timelineUtils.getIunFromTimelineId(timelineId);

        log.debug("Start mapping PnF24PdfSetReadyEvent.Detail iun {}",iunFromTimelineId);
        Map<Integer, List<String>> result = generatedPdfsUrls.stream()
                .collect(Collectors.groupingBy(
                        item -> Integer.parseInt(item.getPathTokens().split(PATH_TOKEN_SEPARATOR)[0]), // Estrae recIndex
                        Collectors.mapping(PnF24PdfSetReadyEventItem::getUri, Collectors.toList())
                ));

        log.debug("Invoke f24Service.handleF24PrepareResponse for iun {}", iunFromTimelineId);
        f24Service.handleF24PrepareResponse(iunFromTimelineId,result);

        log.debug("scheduleEvent POST_ACCEPTED_PROCESSING_COMPLETED for iun {}", iunFromTimelineId);
        schedulerService.scheduleEvent(iunFromTimelineId, Instant.now(), ActionType.POST_ACCEPTED_PROCESSING_COMPLETED);

    }

    private static void addMdcFilter(String iun) {
        HandleEventUtils.addIunToMdc(iun);
    }
}

package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.api.dto.events.PnF24MetadataValidationEndEvent;
import it.pagopa.pn.api.dto.events.PnF24MetadataValidationEndEventPayload;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.exceptions.PnValidationNotValidF24Exception;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24.PnF24Client;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

@Component
@CustomLog
@AllArgsConstructor
public class F24ResponseHandler {
    private TimelineUtils timelineUtils;
    private NotificationValidationActionHandler validationActionHandler;

    public void handleResponseReceived(PnF24MetadataValidationEndEvent.Detail event) {
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

    private static void addMdcFilter(String iun) {
        HandleEventUtils.addIunToMdc(iun);
    }
}

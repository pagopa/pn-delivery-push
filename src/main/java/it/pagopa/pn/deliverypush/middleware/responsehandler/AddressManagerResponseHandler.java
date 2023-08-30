package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.NormalizeItemsResult;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeItemsResultInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.addressmanager.AddressManagerClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.service.mapper.AddressManagerMapper;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

@Component
@CustomLog
@AllArgsConstructor
public class AddressManagerResponseHandler {

    private NotificationValidationActionHandler notificationValidationActionHandler;
    private TimelineUtils timelineUtils;
    
    public void handleResponseReceived( NormalizeItemsResult response ) {
        String iun = timelineUtils.getIunFromTimelineId(response.getCorrelationId());
        addMdcFilter(iun, response.getCorrelationId());

        log.info("Async response received from service {} for {} with correlationId={}",
                AddressManagerClient.CLIENT_NAME, AddressManagerClient.NORMALIZE_ADDRESS_PROCESS_NAME, response.getCorrelationId());
        final String processName = AddressManagerClient.NORMALIZE_ADDRESS_PROCESS_NAME + " response handler";

        if (timelineUtils.checkIsNotificationCancellationRequested(iun)){
            log.warn("Process {} blocked: cancellation requested for iun {}", processName, iun);
            return;
        }

        try {
            log.logStartingProcess(processName);
            NormalizeItemsResultInt normalizeItemsResult = AddressManagerMapper.externalToInternal(response);
            notificationValidationActionHandler.handleValidateAndNormalizeAddressResponse(iun, normalizeItemsResult);
            log.logEndingProcess(processName);
        } catch (Exception ex){
            log.logEndingProcess(processName, false, ex.getMessage());
            throw ex;
        }
        
    }

    private static void addMdcFilter(String iun, String correlationId) {
        HandleEventUtils.addIunToMdc(iun);
        HandleEventUtils.addCorrelationIdToMdc(correlationId);
    }
}

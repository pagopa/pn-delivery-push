package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.NormalizeItemsResult;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeItemsResultInt;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.service.AddressManagerService;
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

        log.logStartingProcess(AddressManagerService.VALIDATE_AND_NORMALIZE_ADDRESS_PROCESS_NAME);
        
        NormalizeItemsResultInt normalizeItemsResult = AddressManagerMapper.externalToInternal(response);
        notificationValidationActionHandler.handleValidateAndNormalizeAddressResponse(iun, normalizeItemsResult);

        log.logEndingProcess(AddressManagerService.VALIDATE_AND_NORMALIZE_ADDRESS_PROCESS_NAME);
    }

    private static void addMdcFilter(String iun, String correlationId) {
        HandleEventUtils.addIunToMdc(iun);
        HandleEventUtils.addCorrelationIdToMdc(correlationId);
    }
}

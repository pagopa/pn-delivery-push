package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.NormalizeItemsResult;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeItemsResultInt;
import it.pagopa.pn.deliverypush.service.mapper.AddressManagerMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class AddressManagerResponseHandler {
    private NotificationValidationActionHandler notificationValidationActionHandler;
    private TimelineUtils timelineUtils;
    
    public void handleResponseReceived( NormalizeItemsResult response ) {
        String iun = timelineUtils.getIunFromTimelineId(response.getCorrelationId());

        log.debug("handleResponseReceived from addressManager correlationId={} - iun={}", response.getCorrelationId(), iun);
        
        NormalizeItemsResultInt normalizeItemsResult = AddressManagerMapper.externalToInternal(response);

        notificationValidationActionHandler.handleValidateAndNormalizeAddressResponse(iun, normalizeItemsResult);
    }
}

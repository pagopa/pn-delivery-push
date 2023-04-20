package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.NormalizeItemsResult;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeItemsResultInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventIdBuilder;
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

        log.info("handleResponseReceived from addressManager correlationId={} - iun={}", response.getCorrelationId(), iun);
        
        NormalizeItemsResultInt normalizeItemsResult = AddressManagerMapper.externalToInternal(response);

        notificationValidationActionHandler.handleValidateAndNormalizeAddressResponse(iun, normalizeItemsResult);
    }

    public String getIunFromCorrelationId(String correlationId)
    {
        //<timelineId = CATEGORY_VALUE>;IUN_<IUN_VALUE>;RECINDEX_<RECINDEX_VALUE>...
        return correlationId.split("\\" + TimelineEventIdBuilder.DELIMITER)[1].replace("IUN_", "");
    }
}

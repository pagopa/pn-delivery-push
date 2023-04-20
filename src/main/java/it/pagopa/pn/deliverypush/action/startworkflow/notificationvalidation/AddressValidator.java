package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeItemsResultInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.exceptions.PnValidationNotValidAddressException;
import it.pagopa.pn.deliverypush.service.AddressManagerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@Slf4j
public class AddressValidator {
    private final AddressManagerService addressManagerService;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    
    public Mono<Void> requestValidateAndNormalizeAddresses(NotificationInt notification){
        String correlationId = TimelineEventId.VALIDATE_NORMALIZE_ADDRESSES_REQUEST.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());
        
       return addressManagerService.normalizeAddresses(notification, correlationId)
                .flatMap( res ->
                    Mono.fromCallable( () -> {
                        log.debug("Get normalize address sync response - iun={}" , notification.getIun());
                        timelineService.addTimelineElement(
                                timelineUtils.buildValidateAndNormalizeAddressTimelineElement(notification, correlationId),
                                notification
                        );
                        return null;
                    })
                );
    }

    public void handleAddressValidation(String iun, NormalizeItemsResultInt normalizeItemsResult){
        normalizeItemsResult.getResultItems().forEach( normalizeResult ->{
            if(normalizeResult.getError() != null){

                String errorMessage = String.format(
                        "Validation failed, address is not valid. Error=%s - iun=%s id=%s ",
                        normalizeResult.getError(),
                        iun,
                        normalizeResult.getId()
                );
                
                log.error(errorMessage);
                throw new PnValidationNotValidAddressException(errorMessage);
            }
        });
        
    }
}

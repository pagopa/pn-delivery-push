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
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@CustomLog
public class AddressValidator {
    private final String VALIDATE_ADDRESS_PROCESS = "Address validation";

    private final AddressManagerService addressManagerService;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    
    public Mono<Void> requestValidateAndNormalizeAddresses(NotificationInt notification){
        log.debug("Start  requestValidateAndNormalizeAddresses - iun={}" , notification.getIun());

        String correlationId = TimelineEventId.VALIDATE_NORMALIZE_ADDRESSES_REQUEST.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());
        
       return addressManagerService.normalizeAddresses(notification, correlationId)
               .then(
                       Mono.fromCallable(() -> {
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
        log.logChecking(VALIDATE_ADDRESS_PROCESS);

        normalizeItemsResult.getResultItems().forEach( normalizeResult ->{
            if(normalizeResult.getError() != null){

                String errorMessage = String.format(
                        "Validation failed, address is not valid. Error=%s - iun=%s id=%s ",
                        normalizeResult.getError(),
                        iun,
                        normalizeResult.getId()
                );

                log.logCheckingOutcome(VALIDATE_ADDRESS_PROCESS, false, errorMessage);

                log.warn(errorMessage);
                throw new PnValidationNotValidAddressException(errorMessage);
            }
        });

        log.logCheckingOutcome(VALIDATE_ADDRESS_PROCESS, true);
    }
}

package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.nationalregistries.CheckTaxIdOKInt;
import it.pagopa.pn.deliverypush.exceptions.PnValidationTaxIdNotValidException;
import it.pagopa.pn.deliverypush.service.NationalRegistriesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class TaxIdPivaValidator {
    private final NationalRegistriesService nationalRegistriesService;
    private final NotificationUtils notificationUtils;
    
    public void validateTaxIdPiva(NotificationInt notification){
        log.debug("Start validateTaxId - iun={} ", notification.getIun());

        notification.getRecipients().forEach( recipient -> {
            int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
            log.info("Start taxIdValidation - iun={} id={}", notification.getIun(), recIndex);
            
            CheckTaxIdOKInt response = nationalRegistriesService.checkTaxId(recipient.getTaxId());
            if (Boolean.FALSE.equals(response.getIsValid()) ){
                log.info("TaxId is not valid - iun={} id={}", notification.getIun(), recIndex);
                
                throw new PnValidationTaxIdNotValidException(
                        response.getErrorCode()
                );
            }

            log.info("TaxId is valid - iun={} id={}", notification.getIun(), recIndex);
        });

        log.debug("End validateTaxId - iun={} ", notification.getIun());
    }
}
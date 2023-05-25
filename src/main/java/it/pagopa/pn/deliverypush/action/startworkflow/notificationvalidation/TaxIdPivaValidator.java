package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.nationalregistries.CheckTaxIdOKInt;
import it.pagopa.pn.deliverypush.exceptions.PnValidationTaxIdNotValidException;
import it.pagopa.pn.deliverypush.service.NationalRegistriesService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@CustomLog
public class TaxIdPivaValidator {
    private final String VALIDATE_TAX_ID_PROCESS = "Validate taxId";

    private final NationalRegistriesService nationalRegistriesService;
    private final NotificationUtils notificationUtils;
    
    public void validateTaxIdPiva(NotificationInt notification){
        log.logChecking(VALIDATE_TAX_ID_PROCESS);

        notification.getRecipients().forEach( recipient -> {
            int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
            log.debug("Start taxIdValidation for specific recipient - iun={} id={}", notification.getIun(), recIndex);
            
            CheckTaxIdOKInt response = nationalRegistriesService.checkTaxId(recipient.getTaxId());
            if (Boolean.FALSE.equals(response.getIsValid()) ){
                log.debug("TaxId is not valid - iun={} id={}", notification.getIun(), recIndex);
                log.logCheckingOutcome(VALIDATE_TAX_ID_PROCESS, false, response.getErrorCode());

                throw new PnValidationTaxIdNotValidException(response.getErrorCode());
            }

            log.debug("TaxId is valid - iun={} id={}", notification.getIun(), recIndex);
        });

        log.logCheckingOutcome(VALIDATE_TAX_ID_PROCESS, true);
    }
}

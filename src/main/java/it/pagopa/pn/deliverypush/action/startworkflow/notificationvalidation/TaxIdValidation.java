package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.nationalregistries.CheckTaxIdOKInt;
import it.pagopa.pn.deliverypush.exceptions.PnTaxIdNotValidException;
import it.pagopa.pn.deliverypush.service.NationalRegistriesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler.TAXID_NOT_VALID;

@Component
@AllArgsConstructor
@Slf4j
public class TaxIdValidation {
    private final NationalRegistriesService nationalRegistriesService;
    private final NotificationUtils notificationUtils;
    
    public void validateTaxId(NotificationInt notification){
        log.debug("Start validateTaxId - iun={} ", notification.getIun());

        notification.getRecipients().forEach( recipient -> {
            int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
            log.info("Start taxIdValidation - iun={} id={}", notification.getIun(), recIndex);
            
            CheckTaxIdOKInt response = nationalRegistriesService.checkTaxId(recipient.getTaxId());
            if (Boolean.FALSE.equals(response.getIsValid()) ){
                log.info("TaxId is not valid - iun={} id={}", notification.getIun(), recIndex);

                CheckTaxIdOKInt.ErrorCodeEnumInt errorCode = response.getErrorCode();

                throw new PnTaxIdNotValidException(
                        TAXID_NOT_VALID,
                        errorCode.getValue()
                );
            }

            log.info("TaxId is valid - iun={} id={}", notification.getIun(), recIndex);
        });
    }
}

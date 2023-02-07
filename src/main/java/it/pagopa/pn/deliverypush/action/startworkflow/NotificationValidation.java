package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class NotificationValidation {
    public static final String FILE_NOTFOUND = PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND;
    public static final String FILE_SHA_ERROR = PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_SHAFILEERROR;
    public static final String TAXID_NOT_VALID = PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TAXID_NOT_VALID;

    private final AttachmentUtils attachmentUtils;
    private final TaxIdValidation taxIdValidation;

    public void validateNotification(NotificationInt notification){
        
        try {
            attachmentUtils.validateAttachment(notification);
            //taxIdValidation.validateTaxId(notification);
        } catch (PnValidationException ex){
            //Errore di validazione della notifica, va portata in rifiutata
            throw ex;
        } catch (RuntimeException ex){
            //Exception generica, viene schedulato un ritentativo
            throw ex;
            //TODO Da implementare, a valle dell'implementazione eliminare il throw dell'exception
        }
        
    }
}

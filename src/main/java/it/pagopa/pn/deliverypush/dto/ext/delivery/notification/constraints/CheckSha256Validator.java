package it.pagopa.pn.deliverypush.dto.ext.delivery.notification.constraints;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CheckSha256Validator implements ConstraintValidator<CheckSha256, NotificationDocumentInt> {

    @Override
    public boolean isValid(NotificationDocumentInt attachment, ConstraintValidatorContext constraintValidatorContext) {
        //TODO Cambiare una volta configurato safeStorages
        
        /*
        String base64 = attachment.getBody(); 
        NotificationDocumentInt.Digests digests = attachment.getDigests();

        boolean isValid = true;
        if( digests != null && base64 != null && !base64.isBlank() ) {
            String expectedSha256Hex = digests.getSha256();
            if( expectedSha256Hex != null && ! expectedSha256Hex.isBlank() ) {
                byte[] data = Base64Utils.decodeFromString( base64 );
                String computedSha256 = DigestUtils.sha256Hex(data);
                isValid = expectedSha256Hex.equalsIgnoreCase(computedSha256);
            }
        }
        return isValid;
         */
        return true;
    }
}

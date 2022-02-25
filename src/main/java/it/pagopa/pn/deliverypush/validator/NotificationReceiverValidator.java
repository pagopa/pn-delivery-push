package it.pagopa.pn.deliverypush.validator;

import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.deliverypush.validator.preloaded_digest_error.DigestEqualityBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;

//TODO Da eliminare, una volta implementata la PN-764
@Component
public class NotificationReceiverValidator {
    private final Validator validator;

    public NotificationReceiverValidator(Validator validator) {
        this.validator = validator;
    }

    public void checkPreloadedDigests(String key, NotificationAttachment.Digests expected, NotificationAttachment.Digests actual) throws PnValidationException {
        Set<ConstraintViolation<DigestEqualityBean>> errors = validator.validate( DigestEqualityBean.builder()
                .key( key )
                .expected( expected )
                .actual( actual )
                .build()
        );
        if( ! errors.isEmpty() ) {
            throw new PnValidationException(key, errors );
        }
    }
}

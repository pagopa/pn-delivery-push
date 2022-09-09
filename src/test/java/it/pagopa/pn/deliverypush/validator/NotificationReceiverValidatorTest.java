package it.pagopa.pn.deliverypush.validator;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

//TODO Da eliminare, una volta implementata la PN-764
class NotificationReceiverValidatorTest {
    private NotificationReceiverValidator validator;

    @BeforeEach
    void initializeValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = new NotificationReceiverValidator( factory.getValidator() );
    }
    
    @Test
    void successAttachmentDigest() {
        validator.checkPreloadedDigests( "paNotificationId/attachmentKey",
                NotificationDocumentInt.Digests.builder().sha256("expected").build(),
                NotificationDocumentInt.Digests.builder().sha256("expected").build()
        );
        //if fails throw exception
    }

    @Test
    void failAttachmentDigest() {
        // Given
        NotificationDocumentInt.Digests expected = NotificationDocumentInt.Digests.builder().sha256("expected").build();
        NotificationDocumentInt.Digests actual = NotificationDocumentInt.Digests.builder().sha256("wrong").build();
        // When
        PnValidationException exc = Assertions.assertThrows( PnValidationException.class, () ->
                validator.checkPreloadedDigests( "paNotificationId/attachmentKey", expected, actual )
        );
        String propPath = exc.getProblem().getErrors().iterator().next().getElement();

        // Then
        Assertions.assertEquals( "paNotificationId/attachmentKey",  propPath );
    }

    private static String propertyPathToString( Path propertyPath ) {
        return propertyPath.toString().replaceFirst(".<[^>]*>$", "");
    }
}
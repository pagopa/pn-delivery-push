package it.pagopa.pn.deliverypush.validator;

import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

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
                NotificationAttachment.Digests.builder().sha256("expected").build(),
                NotificationAttachment.Digests.builder().sha256("expected").build()
        );
        //if fails throw exception
    }

    @Test
    void failAttachmentDigest() {
        // Given
        NotificationAttachment.Digests expected = NotificationAttachment.Digests.builder().sha256("expected").build();
        NotificationAttachment.Digests actual = NotificationAttachment.Digests.builder().sha256("wrong").build();
        // When
        PnValidationException exc = Assertions.assertThrows( PnValidationException.class, () ->
                validator.checkPreloadedDigests( "paNotificationId/attachmentKey", expected, actual )
        );
        Path propPath = exc.getValidationErrors().iterator().next().getPropertyPath();

        // Then
        Assertions.assertEquals( "paNotificationId/attachmentKey", propertyPathToString( propPath ));
    }

    private static String propertyPathToString( Path propertyPath ) {
        return propertyPath.toString().replaceFirst(".<[^>]*>$", "");
    }
}